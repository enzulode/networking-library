## Network interconnection library
#### ```by enzulode```

### Description
This library supports request and response segmentation if request or response size exceeded the defined 
bound (bounds are defined in NetworkUtils class as global constant variable)

### Available modes:
- ```DatagramChannel Server``` and ```DatagramSocket Client```

### Usage

#### With Gradle
First of all, you have to add the repository in your ```build.gradle```
```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/enzulode/networking-library")
    }
}
```

And then specify the dependency
```groovy
dependencies {
    implementation 'com.enzulode:networking-library:<required library version>'
}
```

#### With maven
First of all, you have to add the following dependency in your ```pom.xml```
```xml
<dependency>
  <groupId>com.enzulode</groupId>
  <artifactId>networking-library</artifactId>
  <version>{required library version}</version>
</dependency>
```
And then, install the dependencies
```shell
mvn install
```

