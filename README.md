
Building native image:

```sh
mvn package -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true
```