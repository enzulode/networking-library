## Network interconnection library
#### ```by enzulode```

### Description
This library supports request and response segmentation if request or response size exceeded the defined 
bound (bounds are defined in NetworkUtils class as global constant variable)

### Available clients:
- ```DatagramSocket``` based client
- ```DatagramChannel``` based client

### Available servers:
- ```DatagramSocket``` based server
- ```DatagramChannel``` based server

### Usage

#### With Gradle
First of all, you have to create file ```gradle.properties``` in your project root folder.
Then add following lines there
```properties
gpr.user=<your github username>
gpr.key=<your github token>
```

**Notice:** your token is your private, secret information. Do not upload the ```gradle.properties``` with token in it

Then add the repository in your ```build.gradle```
```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/enzulode/networking-library")
        credentials {
                username = project.findProperty("gpr.user")
                password = project.findProperty("gpr.key")
            }
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

