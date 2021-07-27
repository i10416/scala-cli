import $file.deps, deps.{Deps, Docker}

import java.io.File
import mill._, scalalib._
import scala.util.Properties

lazy val cs: String =
  if (Properties.isWin) {
    val pathExt = Option(System.getenv("PATHEXT"))
      .toSeq
      .flatMap(_.split(File.pathSeparator).toSeq)
    val path = Option(System.getenv("PATH"))
      .toSeq
      .flatMap(_.split(File.pathSeparator))
      .map(new File(_))

    def candidates =
      for {
        dir <- path.iterator
        ext <- pathExt.iterator
      } yield new File(dir, s"cs$ext")

    candidates
      .filter(_.canExecute)
      .toStream
      .headOption
      .map(_.getAbsolutePath)
      .getOrElse {
        System.err.println("Warning: could not find cs in PATH.")
        "cs"
      }
  }
  else
    "cs"

// should be the default index in the upcoming coursier release (> 2.0.16)
def jvmIndex = "https://github.com/coursier/jvm-index/raw/master/index.json"

lazy val vcvarsCandidates = Option(System.getenv("VCVARSALL")) ++ Seq(
  """C:\Program Files (x86)\Microsoft Visual Studio\2019\Enterprise\VC\Auxiliary\Build\vcvars64.bat""",
  """C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat""",
  """C:\Program Files (x86)\Microsoft Visual Studio\2017\Enterprise\VC\Auxiliary\Build\vcvars64.bat""",
  """C:\Program Files (x86)\Microsoft Visual Studio\2017\Community\VC\Auxiliary\Build\vcvars64.bat"""
)

def vcvarsOpt: Option[os.Path] =
  vcvarsCandidates
    .iterator
    .map(os.Path(_, os.pwd))
    .filter(os.exists(_))
    .toStream
    .headOption

final case class DockerParams(
  imageName: String,
  workingDir: os.Path,
  prepareCommand: String,
  csUrl: String,
  extraNativeImageArgs: Seq[String]
)

def generateNativeImage(
  graalVmVersion: String,
  classPath: Seq[os.Path],
  mainClass: String,
  dest: os.Path,
  includeResources: Seq[String],
  dockerParamsOpt: Option[DockerParams] = None
): Unit = {

  val defaultJvmId = s"graalvm-java11:$graalVmVersion"
  val graalVmHome = Option(System.getenv("GRAALVM_HOME")).getOrElse {
    import sys.process._
    Seq(cs, "java-home", "--jvm", defaultJvmId, "--jvm-index", jvmIndex).!!.trim
  }

  val ext = if (Properties.isWin) ".cmd" else ""
  val nativeImage = s"$graalVmHome/bin/native-image$ext"

  if (!os.isFile(os.Path(nativeImage))) {
    val ret = os.proc(s"$graalVmHome/bin/gu$ext", "install", "native-image").call(
      stdin = os.Inherit,
      stdout = os.Inherit
    )
    if (ret.exitCode != 0)
      System.err.println(s"Warning: 'gu install native-image' exited with return code ${ret.exitCode}}")
    if (!os.isFile(os.Path(nativeImage)))
      System.err.println(s"Warning: $nativeImage not found, and not installed by 'gu install native-image'")
  }

  val finalCp =
    if (Properties.isWin) {
      import java.util.jar._
      val manifest = new Manifest
      val attributes = manifest.getMainAttributes
      attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
      attributes.put(Attributes.Name.CLASS_PATH, classPath.map(_.toIO.getAbsolutePath).mkString(" "))
      val jarFile = File.createTempFile("classpathJar", ".jar")
      val jos = new JarOutputStream(new java.io.FileOutputStream(jarFile), manifest)
      jos.close()
      jarFile.getAbsolutePath
    } else
      classPath.map(_.toString).mkString(File.pathSeparator)

  val extraNativeImageArgs =
    if (Properties.isWin)
      Seq(
        "--no-server",
        "-J-Xmx6g",
        "--verbose"
      )
    else Nil

  def command(nativeImage: String, extraNativeImageArgs: Seq[String], dest: String, classPath: String) = Seq(
    nativeImage,
    "--no-fallback"
  ) ++
  extraNativeImageArgs ++
  Seq(
    "--enable-url-protocols=https",
    "--initialize-at-build-time=scala.Symbol",
    "--initialize-at-build-time=scala.Symbol$",
    "--initialize-at-build-time=scala.Function1",
    "--initialize-at-build-time=scala.Function2",
    "--initialize-at-build-time=scala.runtime.LambdaDeserialize",
    "--initialize-at-build-time=scala.runtime.EmptyMethodCache",
    "--initialize-at-build-time=scala.runtime.StructuralCallSite",
    "--initialize-at-build-time=scala.collection.immutable.VM",
    "--initialize-at-build-time=com.google.common.jimfs.SystemJimfsFileSystemProvider",
    "-H:IncludeResources=amm-dependencies.txt",
    "-H:IncludeResources=bootstrap.*.jar",
    "-H:IncludeResources=coursier/coursier.properties",
    "-H:IncludeResources=coursier/launcher/coursier.properties",
    "-H:IncludeResources=coursier/launcher/.*.bat",
    "-H:IncludeResources=org/scalajs/linker/backend/emitter/.*.sjsir"
  ) ++
  includeResources.map(r => s"-H:IncludeResources=$r") ++
  Seq(
    "--allow-incomplete-classpath",
    "--report-unsupported-elements-at-runtime",
    "-H:+ReportExceptionStackTraces",
    s"-H:Name=${dest}",
    "-cp",
    classPath,
    mainClass
  )

  def defaultCommand = command(nativeImage, extraNativeImageArgs, dest.relativeTo(os.pwd).toString, finalCp)

  val (finalCommand, tmpDestOpt) =
    if (Properties.isWin)
      vcvarsOpt match {
        case None =>
          System.err.println(s"Warning: vcvarsall script not found in predefined locations:")
          for (loc <- vcvarsCandidates)
            System.err.println(s"  $loc")
          (defaultCommand, None)
        case Some(vcvars) =>
          // chcp 437 sometimes needed, see https://github.com/oracle/graal/issues/2522
          val escapedCommand = defaultCommand.map {
            case s if s.contains(" ") => "\"" + s + "\""
            case s => s
          }
          val script =
           s"""chcp 437
              |@call "$vcvars"
              |if %errorlevel% neq 0 exit /b %errorlevel%
              |@call ${escapedCommand.mkString(" ")}
              |""".stripMargin
          val scriptPath = os.temp(script.getBytes, prefix = "run-native-image", suffix = ".bat")
          (Seq(scriptPath.toString), None)
      }
    else
      dockerParamsOpt match {
        case Some(params) =>
          var entries = Set.empty[String]
          val cpDir = params.workingDir / "cp"
          if (os.exists(cpDir))
            os.remove.all(cpDir)
          os.makeDir.all(cpDir)
          val copiedCp = classPath.filter(os.exists(_)).map { f =>
            val name =
              if (entries(f.last)) {
                var i = 1
                val (base, ext) = if (f.last.endsWith(".jar")) (f.last.stripSuffix(".jar"), ".jar") else (f.last, "")
                var candidate = ""
                while ({
                  candidate = s"$base-$i$ext"
                  entries(candidate)
                }) {
                  i += 1
                }
                candidate
              }
              else f.last
            entries = entries + name
            val dest = cpDir / name
            os.copy(f, dest)
            s"/data/cp/$name"
          }
          val cp = copiedCp.mkString(File.pathSeparator)
          val escapedCommand = command("native-image", params.extraNativeImageArgs, "/data/scala-cli", cp).map {
            case s if s.contains(" ") => "\"" + s + "\""
            case s => s
          }
          val backTick = "\\"
          val script =
            s"""#!/usr/bin/env bash
               |set -e
               |${params.prepareCommand}
               |eval "$$(/data/cs java --env --jvm "$defaultJvmId" --jvm-index "$jvmIndex")"
               |gu install native-image
               |${escapedCommand.head}""".stripMargin + escapedCommand.drop(1).map("\\\n  " + _).mkString + "\n"
          val scriptPath = params.workingDir / "run-native-image.sh"
          os.write.over(scriptPath, script, createFolders = true)
          os.perms.set(scriptPath, "rwxr-xr-x")
          val csPath = os.Path(os.proc("cs", "get", params.csUrl).call().out.text.trim)
          os.copy.over(csPath, params.workingDir / "cs")
          os.perms.set(params.workingDir / "cs", "rwxr-xr-x")
          val termOpt = if (System.console() == null) Nil else Seq("-t")
          val dockerCmd = Seq("docker", "run") ++ termOpt ++ Seq(
            "--rm",
            "-v", s"${params.workingDir}:/data",
            "-e", "COURSIER_JVM_CACHE=/data/jvm-cache",
            "-e", "COURSIER_CACHE=/data/cs-cache",
            params.imageName,
            "/data/run-native-image.sh"
          )
          (dockerCmd, Some(params.workingDir / "scala-cli"))
        case None =>
          (defaultCommand, None)
      }

  val res = os.proc(finalCommand.map(x => x: os.Shellable): _*).call(
    stdin = os.Inherit,
    stdout = os.Inherit
  )
  if (res.exitCode == 0)
    tmpDestOpt.foreach(tmpDest => os.copy(tmpDest, dest))
  else
    sys.error(s"native-image command exited with ${res.exitCode}")
}

def platformExtension: String =
  if (Properties.isWin) ".exe"
  else ""

def platformExecutableJarExtension: String =
  if (Properties.isWin) ".bat"
  else ""

def platformSuffix: String = {
  val arch = sys.props("os.arch").toLowerCase(java.util.Locale.ROOT) match {
    case "amd64" => "x86_64"
    case other => other
  }
  val os =
    if (Properties.isWin) "pc-win32"
    else if (Properties.isLinux) "pc-linux"
    else if (Properties.isMac) "apple-darwin"
    else sys.error(s"Unrecognized OS: ${sys.props("os.name")}")
  s"$arch-$os"
}


def localRepoResourcePath = "local-repo.zip"

trait CliLaunchers extends SbtModule {

  def localRepoJar: T[PathRef]
  def graalVmVersion: String

  def nativeImageMainClass = T{
    mainClass().getOrElse(sys.error("Don't know what main class to use"))
  }

  def transitiveJars: T[Agg[PathRef]] = {

    def allModuleDeps(todo: List[JavaModule]): List[JavaModule] =
      todo match {
        case Nil => Nil
        case h :: t =>
          h :: allModuleDeps(h.moduleDeps.toList ::: t)
      }

    T{
      mill.define.Target.traverse(allModuleDeps(this :: Nil).distinct)(m =>
        T.task{m.jar()}
      )()
    }
  }

  private def stripFile(jar: os.Path, destDir: os.Path, toStrip: String): Option[os.Path] = {
    import java.io._
    import java.util.zip._
    import scala.collection.JavaConverters._
    var zf: ZipFile = null
    try {
      zf = new ZipFile(jar.toIO)
      val ent = zf.getEntry(toStrip)
      if (ent == null) None
      else {
        os.makeDir.all(destDir)
        val dest = destDir / (jar.last.stripSuffix(".jar") + "-patched.jar")
        var fos: FileOutputStream = null
        var zos: ZipOutputStream = null
        try {
          fos = new FileOutputStream(dest.toIO)
          zos = new ZipOutputStream(fos)
          val buf = Array.ofDim[Byte](64*1024)
          for (ent <- zf.entries.asScala if ent.getName != toStrip) {
            zos.putNextEntry(ent)
            var is: InputStream = null
            try {
              is = zf.getInputStream(ent)
              var read = -1
              while ({
                read = is.read(buf)
                read >= 0
              }) {
                if (read > 0)
                  zos.write(buf, 0, read)
              }
            } finally {
              if (is != null)
                is.close()
            }
          }
          zos.finish()
        } finally {
          if (zos != null) zos.close()
          if (fos != null) fos.close()
        }
        Some(dest)
      }
    } finally {
      if (zf != null)
        zf.close()
    }
  }

  def stripLsp4jPreconditionsFromBsp4j = T{ false }

  def nativeImageClassPath = T{
    val dir = T.dest / "patched-jars"
    val toRemove = "org/eclipse/lsp4j/util/Preconditions.class"
    val baseClassPath = runClasspath()

    if (stripLsp4jPreconditionsFromBsp4j())
      baseClassPath.map { ref =>
        if (ref.path.last.startsWith("bsp4j-"))
          stripFile(ref.path, dir, toRemove).map(PathRef(_)).getOrElse(ref)
        else
          ref
      }
    else
      baseClassPath
  }

  private def doGenerateNativeImage(
    cp: Seq[os.Path],
    mainClass0: String,
    destDir: os.Path,
    localRepoJar0: os.Path,
    overwrite: Boolean = true
  ): os.Path = {
    val dest = destDir / "scala"
    val actualDest = destDir / s"scala$platformExtension"

    if (overwrite || !os.isFile(actualDest))
      generateNativeImage(
        graalVmVersion,
        cp :+ localRepoJar0,
        mainClass0,
        dest,
        Seq(localRepoResourcePath)
      )

    actualDest
  }

  def nativeImage = {
    val isCI = System.getenv("CI") != null
    if (isCI)
      T.persistent {
        val cp = nativeImageClassPath().map(_.path)
        val mainClass0 = nativeImageMainClass()

        val localRepoJar0 = localRepoJar().path

        val executable = doGenerateNativeImage(
          cp,
          mainClass0,
          T.ctx().dest,
          localRepoJar0,
          overwrite = false
        )

        PathRef(executable)
      }
    else
      T{
        val cp = nativeImageClassPath().map(_.path)
        val mainClass0 = nativeImageMainClass()

        val localRepoJar0 = localRepoJar().path

        val executable = doGenerateNativeImage(
          cp,
          mainClass0,
          T.ctx().dest,
          localRepoJar0
        )

        PathRef(executable)
      }
  }

  def nativeImageStatic = T.persistent {
    val cp = nativeImageClassPath().map(_.path)
    val mainClass0 = nativeImageMainClass()
    val dest = T.ctx().dest / "scala"

    val localRepoJar0 = localRepoJar().path

    val params = DockerParams(
      imageName = Docker.muslBuilder,
      workingDir = T.dest / "data",
      prepareCommand = """(cd /usr/local/musl/bin && ln -s *-musl-gcc musl-gcc) && export PATH="/usr/local/musl/bin:$PATH"""",
      csUrl = s"https://github.com/coursier/coursier/releases/download/v${deps.csDockerVersion}/cs-x86_64-pc-linux",
      extraNativeImageArgs = Seq(
        "--static",
        "--libc=musl"
      )
    )

    if (os.exists(dest)) {
      val printableDest = if (dest.startsWith(os.pwd)) dest.relativeTo(os.pwd).toString else dest.toString
      T.log.outputStream.println(s"Warning: not re-computing $printableDest, delete it if you think it's stale")
    } else
      generateNativeImage(
        graalVmVersion,
        cp :+ localRepoJar0,
        mainClass0,
        dest,
        Seq(localRepoResourcePath),
        dockerParamsOpt = Some(params)
      )

    PathRef(dest)
  }

  def nativeImageMostlyStatic = T.persistent {
    val cp = nativeImageClassPath().map(_.path)
    val mainClass0 = nativeImageMainClass()
    val dest = T.ctx().dest / "scala"

    val localRepoJar0 = localRepoJar().path

    val params = DockerParams(
      imageName = "ubuntu:18.04",
      workingDir = T.dest / "data",
      prepareCommand = "apt-get update -q -y && apt-get install -q -y build-essential libz-dev",
      csUrl = s"https://github.com/coursier/coursier/releases/download/v${deps.csDockerVersion}/cs-x86_64-pc-linux",
      extraNativeImageArgs = Seq(
        "-H:+StaticExecutableWithDynamicLibC"
      )
    )

    if (os.exists(dest)) {
      val printableDest = if (dest.startsWith(os.pwd)) dest.relativeTo(os.pwd).toString else dest.toString
      T.log.outputStream.println(s"Warning: not re-computing $printableDest, delete it if you think it's stale")
    } else
      generateNativeImage(
        graalVmVersion,
        cp :+ localRepoJar0,
        mainClass0,
        dest,
        Seq(localRepoResourcePath),
        dockerParamsOpt = Some(params)
      )

    PathRef(dest)
  }

  def runWithAssistedConfig(args: String*) = T.command {
    val cp = jarClassPath().map(_.path).mkString(File.pathSeparator)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))
    val graalVmHome = Option(System.getenv("GRAALVM_HOME")).getOrElse {
      import sys.process._
      Seq(cs, "java-home", "--jvm", s"graalvm-java11:$graalVmVersion", "--jvm-index", jvmIndex).!!.trim
    }
    val outputDir = T.ctx().dest / "config"
    val command = Seq(
      s"$graalVmHome/bin/java",
      s"-agentlib:native-image-agent=config-output-dir=$outputDir",
      "-cp", cp,
      mainClass0
    ) ++ args
    os.proc(command.map(x => x: os.Shellable): _*).call(
      stdin = os.Inherit,
      stdout = os.Inherit
    )
    T.log.outputStream.println(s"Config generated in ${outputDir.relativeTo(os.pwd)}")
  }

  def runFromJars(args: String*) = T.command {
    val cp = jarClassPath().map(_.path).mkString(File.pathSeparator)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))
    val command = Seq("java", "-cp", cp, mainClass0) ++ args
    os.proc(command.map(x => x: os.Shellable): _*).call(
      stdin = os.Inherit,
      stdout = os.Inherit
    )
  }

  def runClasspath = T{
    super.runClasspath() ++ Seq(localRepoJar())
  }

  def jarClassPath = T{
    val cp = runClasspath() ++ transitiveJars()
    cp.filter(ref => os.exists(ref.path) && !os.isDir(ref.path))
  }

  def launcher = T{
    import coursier.launcher.{AssemblyGenerator, BootstrapGenerator, ClassPathEntry, Parameters, Preamble}
    import scala.util.Properties.isWin
    val cp = jarClassPath().map(_.path)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))

    val dest = T.ctx().dest / (if (isWin) "launcher.bat" else "launcher")

    val preamble = Preamble()
      .withOsKind(isWin)
      .callsItself(isWin)
    val entries = cp.map(path => ClassPathEntry.Url(path.toNIO.toUri.toASCIIString))
    val loaderContent = coursier.launcher.ClassLoaderContent(entries)
    val params = Parameters.Bootstrap(Seq(loaderContent), mainClass0)
      .withDeterministic(true)
      .withPreamble(preamble)

    BootstrapGenerator.generate(params, dest.toNIO)

    PathRef(dest)
  }

  def standaloneLauncher = T{

    val cachePath = os.Path(coursier.cache.FileCache().location, os.pwd)
    def urlOf(path: os.Path): Option[String] = {
      if (path.startsWith(cachePath)) {
        val segments = path.relativeTo(cachePath).segments
        val url = segments.head + "://" + segments.tail.mkString("/")
        Some(url)
      }
      else None
    }

    import coursier.launcher.{AssemblyGenerator, BootstrapGenerator, ClassPathEntry, Parameters, Preamble}
    import scala.util.Properties.isWin
    val cp = jarClassPath().map(_.path)
    val mainClass0 = mainClass().getOrElse(sys.error("No main class"))

    val dest = T.ctx().dest / (if (isWin) "launcher.bat" else "launcher")

    val preamble = Preamble()
      .withOsKind(isWin)
      .callsItself(isWin)
    val entries = cp.map { path =>
      urlOf(path) match {
        case None =>
          val content = os.read.bytes(path)
          val name = path.last
          ClassPathEntry.Resource(name, os.mtime(path), content)
        case Some(url) => ClassPathEntry.Url(url)
      }
    }
    val loaderContent = coursier.launcher.ClassLoaderContent(entries)
    val params = Parameters.Bootstrap(Seq(loaderContent), mainClass0)
      .withDeterministic(true)
      .withPreamble(preamble)

    BootstrapGenerator.generate(params, dest.toNIO)

    PathRef(dest)
  }

}

trait HasTests extends SbtModule {
  trait Tests extends super.Tests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.expecty,
      Deps.munit
    )
    def testFramework = "munit.Framework"
    def forkArgs = super.forkArgs() ++ Seq("-Xmx512m", "-Xms128m")
  }
}

trait PublishLocalNoFluff extends PublishModule {
  def emptyZip = T{
    import java.io._
    import java.util.zip._
    val dest = T.dest / "empty.zip"
    val baos = new ByteArrayOutputStream
    val zos = new ZipOutputStream(baos)
    zos.finish()
    zos.close()
    os.write(dest, baos.toByteArray)
    PathRef(dest)
  }
  // adapted from https://github.com/com-lihaoyi/mill/blob/fea79f0515dda1def83500f0f49993e93338c3de/scalalib/src/PublishModule.scala#L70-L85
  // writes empty zips as source and doc JARs
  def publishLocalNoFluff(localIvyRepo: String = null): define.Command[PathRef] = T.command {

    import mill.scalalib.publish.LocalIvyPublisher
    val publisher = localIvyRepo match {
      case null => LocalIvyPublisher
      case repo => new LocalIvyPublisher(os.Path(repo.replace("{VERSION}", publishVersion()), os.pwd))
    }

    publisher.publish(
      jar = jar().path,
      sourcesJar = emptyZip().path,
      docJar = emptyZip().path,
      pom = pom().path,
      ivy = ivy().path,
      artifact = artifactMetadata(),
      extras = extraPublish()
    )

    jar()
  }
}

trait LocalRepo extends Module {

  def stubsModules: Seq[PublishLocalNoFluff]
  def version: T[String]

  def publishStubs = T{
    val tasks = stubsModules.map(_.publishLocalNoFluff())
    define.Task.sequence(tasks)
  }

  def localRepo = T{
    val repoRoot = os.rel / "out" / "repo" / "{VERSION}"
    val tasks = stubsModules.map(_.publishLocalNoFluff(repoRoot.toString))
    define.Task.sequence(tasks)
  }

  def localRepoZip = T{
    val ver = version()
    val something = localRepo()
    val repoDir = os.pwd / "out" / "repo" / ver
    val destDir = T.dest / ver / "repo.zip"
    val dest = destDir / "repo.zip"

    import java.io._
    import java.util.zip._
    os.makeDir.all(destDir)
    var fos: FileOutputStream = null
    var zos: ZipOutputStream = null
    try {
      fos = new FileOutputStream(dest.toIO)
      zos = new ZipOutputStream(new BufferedOutputStream(fos))
      os.walk(repoDir).filter(_ != repoDir).foreach { p =>
        val isDir = os.isDir(p)
        val name = p.relativeTo(repoDir).toString + (if (isDir) "/" else "")
        val entry = new ZipEntry(name)
        entry.setTime(os.mtime(p))
        zos.putNextEntry(entry)
        if (!isDir) {
          zos.write(os.read.bytes(p))
          zos.flush()
        }
        zos.closeEntry()
      }
      zos.finish()
    } finally {
      if (zos != null) zos.close()
      if (fos != null) fos.close()
    }

    PathRef(dest)
  }

  def localRepoJar = T{
    val zip = localRepoZip().path
    val dest = T.dest / "repo.jar"

    import java.io._
    import java.util.zip._
    var fos: FileOutputStream = null
    var zos: ZipOutputStream = null
    try {
      fos = new FileOutputStream(dest.toIO)
      zos = new ZipOutputStream(new BufferedOutputStream(fos))

      val entry = new ZipEntry(localRepoResourcePath)
      entry.setTime(os.mtime(zip))
      zos.putNextEntry(entry)
      zos.write(os.read.bytes(zip))
      zos.flush()
      zos.closeEntry()

      zos.finish()
    } finally {
      if (zos != null) zos.close()
      if (fos != null) fos.close()
    }

    PathRef(dest)
  }

}

private def doFormatNativeImageConf(dir: os.Path, format: Boolean): List[os.Path] = {
  val sortByName = Set("jni-config.json", "reflect-config.json")
  val files = Seq("jni-config.json", "proxy-config.json", "reflect-config.json", "resource-config.json")
  var needsFormatting = List.empty[os.Path]
  for (name <- files) {
    val file = dir / name
    if (os.isFile(file)) {
      val content = os.read(file)
      val json = ujson.read(content)
      val updatedJson =
        if (name == "reflect-config.json")
          json.arrOpt.fold(json) { arr =>
            val values = arr.toVector.groupBy(_("name").str).toVector.sortBy(_._1).map(_._2).map { t =>
              val entries = t.map(_.obj).reduce(_ ++ _)
              if (entries.get("allDeclaredFields") == Some(ujson.Bool(true)))
                entries -= "fields"
              if (entries.get("allDeclaredMethods") == Some(ujson.Bool(true)))
                entries -= "methods"
              ujson.Obj(entries)
            }
            ujson.Arr(values: _*)
          }
        else if (sortByName(name))
          json.arrOpt.fold(json) { arr =>
            val values = arr.toVector.sortBy(_("name").str)
            ujson.Arr(values: _*)
          }
        else
          json
      val updatedContent = updatedJson.render(indent = 2)
      if (updatedContent != content) {
        needsFormatting = file :: needsFormatting
        if (format)
          os.write.over(file, updatedContent)
      }
    }
  }
  needsFormatting
}

trait FormatNativeImageConf extends JavaModule {
  def nativeImageConfDirs = T{
    resources()
      .map(_.path)
      .flatMap { path =>
        os.walk(path / "META-INF" / "native-image")
          .filter(_.last.endsWith("-config.json"))
          .filter(os.isFile(_))
          .map(_ / os.up)
      }
      .distinct
  }
  def checkNativeImageConfFormat() = T.command {
    var needsFormatting = List.empty[os.Path]
    for (dir <- nativeImageConfDirs())
      needsFormatting = doFormatNativeImageConf(dir, format = false) ::: needsFormatting
    if (needsFormatting.nonEmpty) {
      System.err.println(s"Error: ${needsFormatting.length} file(s) needs formatting:")
      for (f <- needsFormatting)
        System.err.println(s"  ${if (f.startsWith(os.pwd)) f.relativeTo(os.pwd).toString else f.toString}")
    }
    ()
  }
  def formatNativeImageConf() = T.command {
    var formattedCount = 0
    for (dir <- nativeImageConfDirs())
      formattedCount += doFormatNativeImageConf(dir, format = true).length
    System.err.println(s"Formatted $formattedCount file(s).")
    ()
  }
}
