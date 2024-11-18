package external;

import com.intuit.karate.junit5.Karate;

class ExternalRunner {

    // @Karate.Test
    // Karate testLogin() {
    // return Karate.run("login").relativeTo(getClass());
    // }

    // // @Karate.Test
    // // Karate testWs() {
    // // return Karate.run("ws").relativeTo(getClass());
    // // }

    // @Karate.Test
    // Karate testCrearLiga() {
    // return Karate.run("crearLiga").relativeTo(getClass());
    // }

    // @Karate.Test
    // Karate testCrearEquipo() {
    // return Karate.run("crearEquipo").relativeTo(getClass());
    // }

    // @Karate.Test
    // Karate testFichar() {
    // return Karate.run("fichar").relativeTo(getClass());
    // }

    // @Karate.Test
    // Karate testMensajes() {
    // return Karate.run("mensajes").relativeTo(getClass());
    // }

    @Karate.Test
    Karate testBanear() {
        return Karate.run("banear").relativeTo(getClass());
    }
}
