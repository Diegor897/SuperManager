# ProyectoIW

**Propuesta proyecto:**
Nuestra aplicación está inspirada en los conocidos juegos LaLiga Fantasy
y SuperManager ACB. 
Nuestra aplicación consiste en obtener el máximo número de puntos a partir de la valoración de los jugadores que forman tus equipos en las jornadas reales de la liga. 
Para formar cada equipo dispones de un presupuesto fijo, con el que puedes fichar hasta diez jugadores. Dependiendo de la valoración de los jugadores en cada jornada, su valor de mercado puede subir o bajar hasta un 15%. De esta manera, podrás aumentar tu presupuesto fichando y vendiendo jugadores cuando estos aumenten de precio, y así mejorar tu equipo.
Puedes formar hasta un máximo de cinco equipos y participar en tantas ligas como quieras, tanto públicas como privadas, y competir contra tus amigos. Solo podrás tener un equipo por cada liga en la que participes. 
La aplicación dispondrá de un foro común en la que se lanzarán noticias, como cuáles son los jugadores con mejor relación valoración/precio, lesiones, etc. 

**Usuarios y contraseñas:**
    - username: a // password: a
    - username: b // password: aa

**Explicaciones adicionales proyecto:**
El proyecto cuenta con los datos de equipos y jugadores de la Liga Endesa hasta la jornada 27. Dichos datos están ya incluidos en el archivo import.sql.

Si se quieren añadir más jornadas hay que ejecutar, por separado y NO desde el directorio del proyecto, el archivo BDdesde0.py.
Este archivo obtendrá los datos de todas las jornadas disponibles en la página web, incluida las fotos de equipos y jugadores, y los guardará en el archivo Liga.db.

Para poder insertar fácilmente los datos en la aplicaicón hay que ejecutar, por separado y NO desde el directorio del proyecto, el archivo parseJson.py.
Este archivo parseará los datos contenidos en Liga.db y los guardará en los siguientes archivos json:
    - jornada_acb.json (Información de los partidos)
    - jugador_acb.json (Información general de los jugadores)
    - puntos_jugador.json (Información del desempeño de los jugadores en las jornadas)
Nota: La ejecución del archivo puede fallar

Después de obtener los archivos json, los administradores podrán subir dichos archivos a la aplicación para añadir las jornadas que no estén ya insertadas en la aplicación.
Como los archivos contienen la información de todas las jornadas, la ejecución de la funcionalidad "Añadir jornadas" tarda unos minutos.

Para la comprobación de dicha funcionalidad se proporcionan ya los ejecutables (.py) y json con la información de todas las jornadas en el directorio /test/java.

Además el proyecto cuenta con el archivo resizeImg.py, para cambiar si fuera necesario el tamaño de las imágenes.
