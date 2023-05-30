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
Properties properties = new Properties()
properties.load(project.rootProject.file('local.properties').newDataInputStream())
def gitUsername = properties.getProperty('gpr.user')
def gitToken = properties.getProperty('gpr.key')

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/enzulode/networking-library")
        credentials {
                username = gitUsername
                password = gitToken
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
