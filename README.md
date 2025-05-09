# Astral

You can use the `addStep` function to add automatic steps which should be performed. Asserts can also be added as steps.

- `addStep` creates a step that runs a method. Completes successfully if no exceptions are caught.
- `addRepeatStep` **TODO** creates a step that runs a method a specified amount of times. Completes successfully if no exceptions are caught.
- `addWaitStep` **TODO** adds a step that waits a specified amount of time before continuing to the next step.
- `addAssert` creates a step that fails if the specified value does not return true.
- `addWaitUntil` adds a step that attempts to run until a condition becomes true, or fails when it times out.

### Example

```kotlin
addStep("Create user") {
    user = User("Maya", 22)
}

addStep("Fetch country") {
    user.fetchCountryFromIPAddress().thenAccept { country ->
        userCountry = country
    }
}

addWaitUntil("Wait until country is fetched", timeout = 10.seconds) { 
    userCountry != null 
}

addAssert("Country is Czech Republic") { userCountry == "Czech Republic" }
```

![image](https://github.com/user-attachments/assets/7bf0c412-4145-485a-83a3-b22bb9f97383)

## Installation

<img src="https://cdn.worldvectorlogo.com/logos/kotlin-2.svg" width="16px"></img>
**Kotlin DSL**
```kotlin
repositories {
    maven {
        name = "devOS"
        url = uri("https://mvn.devos.one/releases")
    }
}

dependencies {
    implementation("cz.lukynka:astral:0.7")
}
```

