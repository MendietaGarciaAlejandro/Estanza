# Estanza

Cliente del coworking [Camar](https://github.com/MendietaGarciaAlejandro/Camar). Corre en
Android, en escritorio y en el navegador, y el código de las pantallas es el mismo en los
tres sitios.

Lo hice para tener un cliente de verdad contra una API que ya conocía por dentro, y para
usar en algo real la librería de validadores que había publicado antes. Lo interesante no
acabó siendo Compose Multiplatform en sí —que funciona mejor de lo que esperaba— sino todo
lo que aparece cuando un cliente y un servidor que escribiste tú tienen que ponerse de
acuerdo en cosas que nadie había escrito en ningún sitio.

## Stack

- Kotlin 2.4.10 y Compose Multiplatform 1.11.1
- Targets: `androidTarget`, `jvm` (escritorio) y `wasmJs` (navegador)
- Ktor 3.5.2 para HTTP, con un motor distinto por plataforma
- Koin 4.2.2 para inyección de dependencias
- multiplatform-settings para las preferencias
- [validadores-es](https://github.com/MendietaGarciaAlejandro/validadores-es), mi propia
  librería, para NIF, NIE, CIF, IBAN, teléfono y código postal

## Cómo levantarlo

Necesita un Camar corriendo. Por defecto apunta a `http://localhost:5106`, salvo en Android,
donde apunta a `http://10.0.2.2:5106` porque es así como el emulador ve el localhost del PC.
Si usas un móvil de verdad hay que cambiarlo, y para eso está la pantalla de ajustes.

```
./gradlew :composeApp:run                              # escritorio
./gradlew :composeApp:assembleDebug                    # APK de Android
./gradlew :composeApp:wasmJsBrowserDevelopmentRun      # navegador
./gradlew :composeApp:jvmTest                          # tests
```

Para escritorio hay que usar `run` y no la recarga en caliente que trae Compose 1.11: esa
va por la tarea `hotRunJvm` y exige un JetBrains Runtime 21. El JBR que trae Android Studio
es más nuevo, y un JDK normal de la 21 no le vale, así que si no tienes un JBR 21 instalado
aparte, la tarea falla antes de arrancar.

Para la versión web, el servidor de desarrollo abre en el 8080 o en el 8081 según cuál esté
libre, y **Camar tiene que permitir ese origen** en `Cors:OrigenesPermitidos`. Si no, el
navegador bloquea las peticiones y en consola no se ve gran cosa. Es el fallo más tonto y el
que más rato me costó la primera vez.

Los tests están en `commonTest`, así que valen para los tres targets. Los ejecuto con
`jvmTest`, que es lo más rápido; no hay tests de interfaz, solo de modelos y de datos.

## Qué hace

Darse de alta, entrar, ver el catálogo de recursos filtrado por tipo, mirar los huecos
libres de cada día, reservar un rato y cancelarlo.

Con una cuenta de administración aparece además una pantalla para cerrar días sueltos
(festivos, obras), dar de alta y de baja recursos, y ver las reservas de todo el coworking.
El apartado solo se pinta si el rol del token lo es, pero eso es comodidad: quien guarda la
puerta es Camar, que contesta 403 a cualquier otro.

La interfaz se coloca según el sitio que haya: barra abajo en un móvil, rail lateral en una
ventana de escritorio o en el navegador, y el catálogo en una columna o en cuatro según
quepa. No hay una versión por plataforma, es la misma midiendo el ancho.

## Decisiones que me parecen las importantes

### Koin, no Hilt

En Ocrea usé Hilt y estoy contento con él, pero aquí no vale: genera el código con un
procesador de anotaciones que solo entiende de Android. Koin resuelve en tiempo de ejecución
y por eso funciona igual en los tres targets, a cambio de que un error de cableado salte al
arrancar y no al compilar. Es un intercambio real y me parece que aquí sale a cuenta.

### La dirección del servidor se cambia desde la propia aplicación

Camar no está desplegado en ningún sitio fijo: corre en el portátil, en una máquina virtual,
o donde toque, y la IP cambia cada dos por tres. Dejarla escrita en el código obligaba a
recompilar las tres versiones cada vez. Ahora se guarda como preferencia, cada plataforma
tiene su valor por defecto, y hay un botón que manda una petición de verdad para comprobar
si al otro lado hay alguien.

Ese botón trata un 401 como éxito, y es a propósito: significa que el servidor está ahí y ha
entendido la petición, solo que todavía no hay sesión. Es justo lo que quieres saber cuando
estás configurando la dirección y aún no tienes credenciales.

### Las horas de Camar se leen en UTC

Esta es la que más me hizo pensar. Camar manda los huecos con desfase `+00:00`, pero lo que
quiere decir es *hora local del coworking*: la apertura de las ocho llega como
`08:00:00+00:00`. Es una simplificación que está documentada en el propio servidor.

Lo natural en el cliente habría sido parsear a `Instant` y convertir a la zona del
dispositivo. En agosto en España eso corre las horas dos posiciones y el usuario ve que el
coworking abre a las diez y cierra a las once de la noche. Así que las horas se leen en UTC,
y esa decisión vive en un único fichero con el porqué escrito al lado. El día que Camar mande
instantes de verdad, se cambia ahí y ya.

### El dinero nunca es un `Double`

Un precio no es una medida aproximada: es una cantidad exacta que acaba en una factura, y en
coma flotante 0.1 + 0.2 no da 0.3. En `commonMain` no hay `BigDecimal`, así que escribí un
serializador que conserva el texto literal del JSON y lo paso a céntimos con enteros solo
para pintarlo.

Esto resolvió de paso algo que descubrí comparando respuestas: Camar manda el mismo precio
con dos decimales al listar (`18.00`) y con tres al crear (`18.000`). Con `Double` habría
dado igual; con texto había que tratarlo.

### Los mensajes del servidor no se reescriben

Camar contesta con ProblemDetails y sus mensajes ya están redactados para leerse: *"Ese hueco
ya esta reservado."*, *"El plan Bono de dia reserva como mucho con un dia de antelacion."*.
La aplicación los enseña tal cual.

La tentación era traducirlos a algo más bonito, o adelantarse a ellos comprobando las reglas
en el cliente. No lo hice porque esas reglas son del servidor y pueden cambiar sin que el
cliente se entere: acabarías enseñando una condición que ya no es cierta. Lo único que sí se
comprueba antes de salir a la red son los documentos y las cuentas bancarias, y eso es
distinto —el algoritmo del NIF no va a cambiar nunca—.

### Ahí es donde entra validadores-es

El formulario de alta valida NIF, NIE, CIF, IBAN, teléfono y código postal con la librería
que publiqué en Maven Central. Un DNI con la letra cambiada se caza sin gastar una petición,
y los ocho campos se señalan todos a la vez en vez de de uno en uno.

El servidor valida exactamente lo mismo por su cuenta, y esa es la validación que cuenta: no
puede fiarse de un cliente al que cualquiera puede sustituir. Lo de aquí es para no molestar
al servidor con algo que ya sabemos que está mal, y para poder señalar el campo concreto.

Un detalle que no esperaba: Android resuelve `validadores-es-jvm`. La librería solo publica
targets `jvm` y `wasmJs`, pero la regla de compatibilidad de Kotlin deja que `androidJvm`
consuma variantes `jvm`, así que funciona en Android sin tener un target de Android.

### Dos grafos de navegación, no uno

Hay un grafo para cuando no hay sesión (acceso, alta, ajustes) y otro para cuando la hay, y
lo que decide cuál se ve es el `StateFlow` del almacén de sesión.

La alternativa era un grafo único con `popUpTo` al entrar y al salir, pero entonces hay que
acordarse de limpiar la pila en dos sitios, y además cuando el token caduca solo. Así hay una
única fuente de verdad: se abre sesión y entras, se cierra —a mano, o porque un 401 la
tiró— y sales, sin pila que arrastre pantallas de la sesión anterior.

### La misma aplicación, dos formas de moverse por ella

Dentro del grafo de sesión, la navegación vive en un armazón que mide el ancho disponible:
por debajo de 700 dp pone una barra abajo, que es donde llega el pulgar en un móvil, y por
encima un rail a la izquierda, que es lo que se espera en una ventana grande. El catálogo va
en una rejilla que se reparte sola según lo que quepa.

El ancho lo mide un `BoxWithConstraints` y no las clases de tamaño de ventana de Material:
esa librería todavía va por versiones alpha para multiplataforma, y para elegir entre dos
disposiciones basta con mirar cuántos dp hay.

Cerrar sesión se fue a ajustes en vez de quedarse en la barra. Es una acción de una vez cada
mucho, y tenerla siempre a un toque de distancia solo servía para pulsarla sin querer.

### El estilo no es el de Material por defecto

Paleta terracota y oliva, redondeos generosos y una escala de texto propia, con la estructura
de listas agrupadas: un bloque blanco sobre fondo cálido con separadores finos por dentro, en
vez de una tarjeta con borde por cada elemento. Cada tipo de espacio tiene su color, para
distinguir una sala de una mesa sin leer la etiqueta.

No se empaqueta ninguna fuente: se usa la del sistema, que en cada plataforma es la que el
usuario espera. Daría más carácter, pero engorda la descarga de la versión web y no
compensaba.

La ficha de un espacio enseñaba antes los huecos como "08:00 - 08:30", y una jornada entera
eran veintiséis fichas con la misma pinta: imposible de recorrer con la vista. Ahora van
separados en mañana y tarde y con la hora de empiece nada más, que se leen como un horario,
que es lo que son. El rango completo se dice abajo al elegir.

### La pantalla de reservar tuvo que cambiar por una regla del servidor

Empecé pensando en huecos de media hora pulsables de uno en uno. No sirve: una mesa flexible
tiene un mínimo de cuatro horas en las reglas de Camar, así que reservar un bloque suelto
daba 422 siempre. Ahora tocas un hueco, tocas otro más tarde, y se selecciona el rango
entero.

El cliente sí comprueba que los huecos elegidos sean **realmente consecutivos**: Camar los
devuelve ordenados, pero entre dos puede haber un rato ya reservado por otro. Si intentas
saltar por encima, en vez de inventarse un rango que pisaría esa reserva, empieza una
selección nueva desde donde tocaste. Eso no es política del servidor, es una verdad del
problema, y por eso sí se comprueba aquí.

## Tres cosas que solo se ven hablando con el servidor de verdad

Levanté Camar y comparé lo que asumían mis tests con lo que contesta. Las tres las encontré así:

**Los errores no vienen como `application/json` sino como `application/problem+json`**, que es
lo que manda la RFC 7807. Si no lo registras aparte en `ContentNegotiation`, Ktor no reconoce
el tipo y el cuerpo del error se pierde entero: te quedas con el código de estado y sin el
mensaje.

**El 401 de un endpoint protegido sin token no trae cuerpo ninguno**, ni siquiera
content-type. Lo emite el middleware de JWT, no el manejador de excepciones de la aplicación,
así que el traductor de errores tiene que aguantar eso y caer a un mensaje por defecto.

**`expiresAt` llega con siete decimales de segundo**, que es como los escribe .NET. Mis
pruebas usaban una fecha redonda. `Instant.parse` lo traga, pero eso hay que comprobarlo, no
suponerlo.

## Y una cosa que solo se ve escribiendo los tests

Seis tests de los modelos fallaban y cada arreglo destapaba el siguiente:

1. El dispatcher de `Main` y el de `runTest` tenían planificadores distintos, así que
   `advanceUntilIdle()` movía uno y las corrutinas estaban en el otro.
2. Aun compartiéndolo seguía sin funcionar: el motor de Ktor resuelve en `Dispatchers.Default`,
   en hilos de verdad, y adelantar el tiempo *virtual* no espera a hilos reales. Hay que
   esperar a que cambie el estado, no al planificador.
3. Y al poner `withTimeout` para no colgarme, saltaba siempre: dentro de `runTest` los
   timeouts también van con tiempo virtual, que el planificador adelanta de golpe.

Los tres están comentados donde tocan, porque son de los que se olvidan y se vuelven a sufrir.

Más tarde apareció el cuarto, y es el que más me costó ver: **esperar a que un indicador de
"cargando" se apague es una carrera**. Si la respuesta llega antes de que el test se suscriba
al flujo, la espera no ve nunca el cambio y se queda colgada hasta que salta el límite de
tiempo. Los tests que lo hacían ahora esperan a un dato que solo puede venir de la respuesta
que les interesa. Uno de ellos, además, nunca probó lo que decía: el envoltorio del entorno
de prueba interceptaba la petición antes de llegar a la respuesta que yo había preparado.

## Y tres más del rediseño de la interfaz

**Nadie pintaba el fondo.** Antes cada pantalla era un `Surface` que lo pintaba; al
reestructurar la navegación los quité y no puse nada en su lugar. Con el sistema en oscuro
salía el tema oscuro dibujado encima del fondo blanco del HTML: cajas de texto negras sobre
blanco. Ahora hay un `Surface` en la raíz, y el `index.html` responde a
`prefers-color-scheme` para que no haya un parpadeo blanco al arrancar.

**`fillMaxWidth().widthIn(max = 460.dp)` no recorta nada.** `fillMaxWidth` deja la anchura
fijada a la del padre y el `widthIn` de detrás ya no puede hacer nada contra un mínimo que
ya vale lo mismo que el máximo. Va al revés: primero el tope, después rellenar hasta él. El
código parece correcto y por eso cuesta verlo.

**Los modelos de las pestañas sobreviven entre visitas.** Con `saveState` y `restoreState`,
cada apartado recuerda por dónde ibas — y de paso mantiene vivo su modelo. Los de reservas y
catálogo solo cargaban en su `init`, así que hacías una reserva, volvías a la pestaña y
seguía diciendo que no habías reservado nada. Ahora recargan al entrar, cancelando la
consulta anterior para que dos cargas solapadas no se pisen.

Ese último lo encontré usando la aplicación, no con los tests, y es el que más me gusta de
los tres: es exactamente el tipo de fallo que aparece cuando cambias la forma de navegar y
no repasas quién sobrevive a qué.

## Limitaciones conocidas

- **El token se guarda sin cifrar.** Va a las mismas preferencias que la dirección del
  servidor, que en Android son SharedPreferences y en el navegador el localStorage. Con un
  token de vida corta y sin refresco es asumible para un proyecto así, pero en algo real el
  de Android tendría que ir a EncryptedSharedPreferences.
- **No funciona sin conexión.** El catálogo se guarda en memoria mientras la aplicación está
  abierta y se olvida al cerrarla. Guardarlo entre arranques ya sería trabajo de una base de
  datos local, y aquí no hacía falta.
- **La lista de huecos vacía no distingue "cerrado" de "completo".** Camar contesta lo mismo
  en los dos casos y desde el cliente no hay forma de saberlo. Podría repetir el horario de
  apertura aquí para adivinarlo, pero prefiero decir lo que sé.
- **La administración no enseña nombres de socio.** Camar devuelve el id del usuario en las
  reservas pero no su nombre, así que en esa pantalla se ve el principio del identificador y
  poco más. Se arreglaría en el servidor, no aquí.
- **La versión web pesa mucho.** Ya optimizada son unos 13,5 MB de descarga: 4,7 del código
  de la aplicación, 8,3 de Skiko —el motor gráfico de Compose, que va aparte y no se puede
  adelgazar desde aquí— y medio mega de JavaScript. Es el punto flojo de Compose en el
  navegador y no tiene arreglo por mi parte; para una aplicación pública de verdad habría
  que plantearse otra cosa para la web.
- **No hay tests de interfaz.** Los 87 tests cubren los modelos de pantalla, el cliente HTTP y
  el formateo; las pantallas en sí no se prueban. Los dos fallos de disposición del rediseño
  no los habría cazado ningún test de los que hay, y por eso están contados arriba.
- **No hay target de iOS.** Compose Multiplatform lo soporta, pero no tengo Mac.
- **Solo habla español.** El formateo de fechas está escrito a mano porque el formateo por
  locale no funciona igual en las tres plataformas, y arrastrar una librería de
  internacionalización para una aplicación de un solo idioma no compensaba.

## Los tres proyectos

Este es el tercero de una serie que encaja:

- **[Camar](https://github.com/MendietaGarciaAlejandro/Camar)** — la API, en .NET
- **[validadores-es](https://github.com/MendietaGarciaAlejandro/validadores-es)** — la
  librería de validadores españoles, publicada en Maven Central
- **Estanza** — este cliente, que usa las dos
