# Sofia2, API Java

*Ver en otros idiomas: [Inglés](README.md), [Español](README.es.md).*

## Copyright

© 2013-16 Minsait

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

## Documentación del API

Antes de utilizar el API SSAP por primera vez, le recomendamos que se familiarice con los conceptos básicos de la plataforma Sofia2. Están incluidos en la
documentación de desarrollo de Sofia2, que puede descargarse desde http://sofia2.com/desarrollador.html.

## Instrucciones de compilación

El API Java se distribuye como un proyecto maven. Para generar el artefacto e instalarlo en el repositorio maven local, basta con ejecutar los
siguientes comandos.

```
cd <raíz de su copia del repositorio>
mvn clean package [-Dmaven.test.skip]
mvn install:install-file -DpomFile=pom.xml -Dfile=target/client-api-<API version>.jar
```

Durante el proceso de compilación, se generará un fichero JAR que contiene el API y sus dependencias. Este fichero se depositará en la ruta

```
<raíz de su copia del repositorio>/target/client-api-standalone-<API-version>.jar
```

## Información de contacto

Si necesita recibir soporte, puede contactar con nosotros en www.sofia2.com o enviando un correo electrónico a [plataformasofia2@indra.es](mailto:plataformasofia2@indra.es).

Además, si desea contribuir al API, no dude en enviarnos una pull request.
