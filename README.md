# ABC Logger: For the collection of a variety of smartphone data

## How-to-Use
* Check this [Link](https://docs.google.com/presentation/d/1Spsh91PjZ-rfkQiY6rQLb5tOxaEkIvrDn3eZffrZgs4/edit?usp=sharing)

## Directory Structure
- [app](app)
  - [grpc](app/grpc): Sub-module for gRPC specification ([Link](https://github.com/woohyeok-choi/ABC-Logger-gRPC-Specs))
    - Checkout whenever the sub-module is updated.
  - abc-logger-client.json: Secrets for server communications and android key-store (not shown publicly)
  - google-services.json: Secrets for Firebase and Google APIs (not shown publicly)
  - [libs](app/libs): Additional libraries (e.g., polar-ble-sdk.jar for Polar H10 Communication).
  
- [source](app/src/main/kotlin/kaist/iclab/abclogger)
  - [adapter](app/src/main/kotlin/kaist/iclab/abclogger/adapter): ViewBinding adapters
  - [collector](app/src/main/kotlin/kaist/iclab/abclogger/collector): Implementation of data collectors
  - [commons](app/src/main/kotlin/kaist/iclab/abclogger/commons): Common functions
  - [core](app/src/main/kotlin/kaist/iclab/abclogger/core): Core functions (e.g., authorization, data upload, notification, preferences).
  - [dialog](app/src/main/kotlin/kaist/iclab/abclogger/dialog): Simple dialog builder
  - [structure](app/src/main/kotlin/kaist/iclab/abclogger/structure): Data structure for shared-use (not same as database).
    - [config](app/src/main/kotlin/kaist/iclab/abclogger/structure/config): Structure for configuration items
    - [survey](app/src/main/kotlin/kaist/iclab/abclogger/structure/survey): Structure for survey questions
  - [ui](app/src/main/kotlin/kaist/iclab/abclogger/ui): Implementation for activities and fragments
    - [base](app/src/main/kotlin/kaist/iclab/abclogger/ui/base): Abstract activities and fragments
    - [config](app/src/main/kotlin/kaist/iclab/abclogger/ui/config): Config fragments
    - [main](app/src/main/kotlin/kaist/iclab/abclogger/ui/main): Main activity
    - [settings](app/src/main/kotlin/kaist/iclab/abclogger/ui/settings): Customized setting activities for some data (e.g., typing speed, Polar H10, survey)
    - [splash](app/src/main/kotlin/kaist/iclab/abclogger/ui/splash): Splash activity; initial authorization and permission requests.
    - [survey](app/src/main/kotlin/kaist/iclab/abclogger/ui/survey): Survey list and response fragments
  - [view](app/src/main/kotlin/kaist/iclab/abclogger/view): Custom view implementation

## How-to-Make a Survey
* Check [this](guides/survey-instruction.md)

## Related Projects
* [ABC Logger Server](https://github.com/woohyeok-choi/ABC-Logger-Server)
* [ABC Logger gRPC specification](https://github.com/woohyeok-choi/ABC-Logger-gRPC-Specs)
* [ABC Logger gRPC communication for Python](https://github.com/woohyeok-choi/ABC-Logger-CRUD-Boilerplate)

## Secrets (for qualified members only)
* Check [this](https://docs.google.com/document/d/1h7MI8P9RrywgHGY0U7LNLkN_geRb_D54BSLYjqgxKjs)

