"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[9306],{3905:function(e,t,n){n.d(t,{Zo:function(){return p},kt:function(){return f}});var s=n(7294);function a(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function o(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var s=Object.getOwnPropertySymbols(e);t&&(s=s.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,s)}return n}function i(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?o(Object(n),!0).forEach((function(t){a(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):o(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function r(e,t){if(null==e)return{};var n,s,a=function(e,t){if(null==e)return{};var n,s,a={},o=Object.keys(e);for(s=0;s<o.length;s++)n=o[s],t.indexOf(n)>=0||(a[n]=e[n]);return a}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(s=0;s<o.length;s++)n=o[s],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(a[n]=e[n])}return a}var l=s.createContext({}),u=function(e){var t=s.useContext(l),n=t;return e&&(n="function"==typeof e?e(t):i(i({},t),e)),n},p=function(e){var t=u(e.components);return s.createElement(l.Provider,{value:t},e.children)},c={inlineCode:"code",wrapper:function(e){var t=e.children;return s.createElement(s.Fragment,{},t)}},m=s.forwardRef((function(e,t){var n=e.components,a=e.mdxType,o=e.originalType,l=e.parentName,p=r(e,["components","mdxType","originalType","parentName"]),m=u(n),f=a,d=m["".concat(l,".").concat(f)]||m[f]||c[f]||o;return n?s.createElement(d,i(i({ref:t},p),{},{components:n})):s.createElement(d,i({ref:t},p))}));function f(e,t){var n=arguments,a=t&&t.mdxType;if("string"==typeof e||a){var o=n.length,i=new Array(o);i[0]=m;var r={};for(var l in t)hasOwnProperty.call(t,l)&&(r[l]=t[l]);r.originalType=e,r.mdxType="string"==typeof e?e:a,i[1]=r;for(var u=2;u<o;u++)i[u]=n[u];return s.createElement.apply(null,i)}return s.createElement.apply(null,n)}m.displayName="MDXCreateElement"},3162:function(e,t,n){n.r(t),n.d(t,{assets:function(){return p},contentTitle:function(){return l},default:function(){return f},frontMatter:function(){return r},metadata:function(){return u},toc:function(){return c}});var s=n(3117),a=n(102),o=(n(7294),n(3905)),i=["components"],r={title:"Filter the test suites to run",sidebar_position:11},l=void 0,u={unversionedId:"cookbooks/test-only",id:"cookbooks/test-only",title:"Filter the test suites to run",description:"The --test-only option is useful for when you only want to run a specific subset of tests.",source:"@site/docs/cookbooks/test-only.md",sourceDirName:"cookbooks",slug:"/cookbooks/test-only",permalink:"/docs/cookbooks/test-only",draft:!1,editUrl:"https://github.com/Virtuslab/scala-cli/edit/main/website/docs/cookbooks/test-only.md",tags:[],version:"current",sidebarPosition:11,frontMatter:{title:"Filter the test suites to run",sidebar_position:11},sidebar:"tutorialSidebar",previous:{title:"Debugging with Scala CLI",permalink:"/docs/cookbooks/debugging"},next:{title:"Command-line options",permalink:"/docs/reference/cli-options"}},p={},c=[{value:"Filter the test suites",id:"filter-the-test-suites",level:2},{value:"Running a single test case by name",id:"running-a-single-test-case-by-name",level:2},{value:"Munit",id:"munit",level:3},{value:"Utest",id:"utest",level:3}],m={toc:c};function f(e){var t=e.components,n=(0,a.Z)(e,i);return(0,o.kt)("wrapper",(0,s.Z)({},m,n,{components:t,mdxType:"MDXLayout"}),(0,o.kt)("p",null,"The ",(0,o.kt)("inlineCode",{parentName:"p"},"--test-only")," option is useful for when you only want to run a specific subset of tests."),(0,o.kt)("p",null,"In this cookbook, we will walk through how to use the ",(0,o.kt)("inlineCode",{parentName:"p"},"--test-only")," option with the ",(0,o.kt)("inlineCode",{parentName:"p"},"test")," sub-command in Scala CLI,\nspecifically when using the ",(0,o.kt)("inlineCode",{parentName:"p"},"munit")," and ",(0,o.kt)("inlineCode",{parentName:"p"},"utest")," test frameworks."),(0,o.kt)("h2",{id:"filter-the-test-suites"},"Filter the test suites"),(0,o.kt)("p",null,"The ",(0,o.kt)("inlineCode",{parentName:"p"},"--test-only")," option in Scala CLI supports using glob patterns to filter test suites to run. A glob pattern is a\nstring which contains asterisk ",(0,o.kt)("inlineCode",{parentName:"p"},"*")," characters to match a set of test suites."),(0,o.kt)("p",null,"Here are three examples of glob patterns for how to filter test suites with ",(0,o.kt)("inlineCode",{parentName:"p"},"--test-only"),":"),(0,o.kt)("ul",null,(0,o.kt)("li",{parentName:"ul"},"start with ",(0,o.kt)("inlineCode",{parentName:"li"},"test")," - ",(0,o.kt)("inlineCode",{parentName:"li"},"test*")),(0,o.kt)("li",{parentName:"ul"},"end with ",(0,o.kt)("inlineCode",{parentName:"li"},"test")," - ",(0,o.kt)("inlineCode",{parentName:"li"},"*test")),(0,o.kt)("li",{parentName:"ul"},"contains ",(0,o.kt)("inlineCode",{parentName:"li"},"test")," -  ",(0,o.kt)("inlineCode",{parentName:"li"},"*test*"))),(0,o.kt)("admonition",{type:"note"},(0,o.kt)("p",{parentName:"admonition"},"The ",(0,o.kt)("inlineCode",{parentName:"p"},"--test-only")," option is supported for every test framework running with Scala CLI.")),(0,o.kt)("p",null,"For example, passing ",(0,o.kt)("inlineCode",{parentName:"p"},"tests.only*")," to the ",(0,o.kt)("inlineCode",{parentName:"p"},"--test-only")," option runs only the test suites which start with ",(0,o.kt)("inlineCode",{parentName:"p"},"tests.only"),":"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=BarTests.scala",title:"BarTests.scala"},'//> using lib "org.scalameta::munit::0.7.29"\npackage tests.only\n\nclass BarTests extends munit.FunSuite {\n  test("bar") {\n    assert(2 + 3 == 5)\n  }\n}\n')),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=HelloTests.scala",title:"HelloTests.scala"},'package tests\n\nclass HelloTests extends munit.FunSuite {\n  test("hello") {\n    assert(2 + 2 == 4)\n  }\n}\n')),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli test . --test-only 'tests.only*' \n# tests.only.BarTests:\n#   + bar 0.045s\n")),(0,o.kt)("h2",{id:"running-a-single-test-case-by-name"},"Running a single test case by name"),(0,o.kt)("admonition",{type:"note"},(0,o.kt)("p",{parentName:"admonition"},"Test frameworks may have their own specific API for specifying the test cases to run aside from the test-only input.")),(0,o.kt)("h3",{id:"munit"},"Munit"),(0,o.kt)("p",null,"To run a specific test case inside a test suite pass ",(0,o.kt)("inlineCode",{parentName:"p"},"*test-name*")," as an argument to ",(0,o.kt)("inlineCode",{parentName:"p"},"scala-cli"),":"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=MunitTests.scala",title:"MunitTests.scala"},'//> using lib "org.scalameta::munit::0.7.29"\npackage tests.only\n\nclass Tests extends munit.FunSuite {\n  test("bar") {\n    assert(2 + 2 == 5)\n  }\n  test("foo") {\n    assert(2 + 3 == 5)\n  }\n  test("foo-again") {\n    assert(2 + 3 == 5)\n  }\n}\n')),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli test .  -- '*foo*'\n# tests.only.Tests:\n#   + foo 0.045s\n#   + foo-again 0.001s\n")),(0,o.kt)("p",null,"This will only run the test which contains the name ",(0,o.kt)("inlineCode",{parentName:"p"},"foo"),", any other tests in your test suite will be skipped."),(0,o.kt)("h3",{id:"utest"},"Utest"),(0,o.kt)("p",null,"Unfortunately, the ",(0,o.kt)("inlineCode",{parentName:"p"},"utest")," test framework doesn't support using glob pattern ",(0,o.kt)("inlineCode",{parentName:"p"},"*")," to filter the test cases to run. In\norder to run a specific test case you will need to specify the exact name of the test you want to run."),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=MyTests.scala",title:"MyTests.scala"},'//> using lib "com.lihaoyi::utest::0.7.10"\n\nimport utest._\n\nobject MyTests extends TestSuite {\n  val tests = Tests {\n    test("foo") {\n      assert(2 + 2 == 4)\n      println("Hello from " + "tests")\n    }\n    test("bar") {\n      assert(2 + 2 == 4)\n      println("Hello from " + "tests")\n    }\n  }\n}\n')),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli test .  -- 'MyTests.foo'\n# -------------------------- Running Tests MyTests.foo --------------------------\n# Hello from tests\n# + MyTests.foo 8ms  \n# Tests: 1, Passed: 1, Failed: 0\n")),(0,o.kt)("p",null,"This will run only the test case with the name ",(0,o.kt)("inlineCode",{parentName:"p"},"MyTests.foo"),"."))}f.isMDXComponent=!0}}]);