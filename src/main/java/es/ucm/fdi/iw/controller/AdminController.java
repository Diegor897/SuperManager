package es.ucm.fdi.iw.controller;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.EquipoACB;
import es.ucm.fdi.iw.model.Jornada;
import es.ucm.fdi.iw.model.JugadorACB;
import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.PartidoACB;
import es.ucm.fdi.iw.model.PuntosEquipo;
import es.ucm.fdi.iw.model.PuntosJugador;
import es.ucm.fdi.iw.model.User;

/**
 * Site administration.
 *
 * Access to this end-point is authenticated - see SecurityConfig
 */
@Controller
@RequestMapping("admin")
public class AdminController {

    private static final Logger log = LogManager.getLogger(AdminController.class);

    @Autowired
    private EntityManager entityManager;

    @GetMapping("/")
    public String index(Model model) {
        List<Message> reportedMessages = entityManager.createNamedQuery("Message.reportados", Message.class)
                .getResultList();
        model.addAttribute("reportados", reportedMessages);
        return "admin";
    }

    @GetMapping("/usuarios")
    public String getUsuarios(Model model) {
        List<User> usuarios = entityManager.createNamedQuery("User.allUsers", User.class)
                .getResultList();

        model.addAttribute("usuarios", usuarios);
        return "usuarios";
    }

    @GetMapping("/mensajesReportados")
    public String mensajesReportados(Model model) {
        List<Message> reportedMessages = entityManager.createNamedQuery("Message.reportados", Message.class)
                .getResultList();
        model.addAttribute("reportados", reportedMessages);

        return "mensajesReportados";
    }

    @GetMapping("/mensajesUsuario")
    public String mensajesReportados(@RequestParam("idUsuario") Long idUsuario, Model model) {
        User u = entityManager.find(User.class, idUsuario);

        List<Message> mensajesUsuario = u.getSent();

        model.addAttribute("usuario", u);
        model.addAttribute("mensajes", mensajesUsuario);

        return "mensajesUsuario";
    }

    @GetMapping("/reiniciarApp")
    public String reiniciarApp() {
        return "reiniciarApp";
    }

    @GetMapping("/anadirJornadas")
    public String añadirJornadas() {
        return "anadirJornadas";
    }

    @Transactional
    public void actualizarDatos() {
        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();

        // Actualizamos las estadísticas de los jugadores
        List<JugadorACB> jugadores = entityManager.createNamedQuery("JugadorACB.jugadores", JugadorACB.class)
                .getResultList();

        for (JugadorACB j : jugadores) {
            try {
                PuntosJugador pJ = entityManager.createNamedQuery("PuntosJugador.jugador", PuntosJugador.class)
                        .setParameter("nombre", j.getNombre())
                        .setParameter("jornada", jornada.getJornada())
                        .getSingleResult();

                int partidosJugados = j.getPartidosJugados();
                int puntosJugador = j.getPuntosTotales();
                int val_jugador = j.getValoracionTotal();

                j.setPartidosJugados(partidosJugados + 1);
                j.setPuntosTotales(puntosJugador + pJ.getPuntos());
                j.setValorMercado(pJ.getValor_mercado());
                j.setValoracionTotal(val_jugador + pJ.getValoracion());
                double valoracionMedia = Math.round((j.getValoracionTotal() / j.getPartidosJugados()));
                j.setValoracionMedia(valoracionMedia);

                entityManager.flush();
            } catch (NoResultException e) {
                /* Si la consulta no devuelve un resultado sigo mirando */ }
        }

        // Actualizamos los puntos de todos los equipos de los usuarios
        List<Equipo> equipos = entityManager.createNamedQuery("Equipo.todosEquipos", Equipo.class)
                .getResultList();

        for (Equipo e : equipos) {
            PuntosEquipo pE = new PuntosEquipo();
            pE.setEquipo(e);
            pE.setJornada(jornada.getJornada());
            List<JugadorACB> jugadoresEquipo = e.getJugadores();
            int puntosGanados = 0;
            for (JugadorACB j : jugadoresEquipo) {
                try {
                    PuntosJugador pJ = entityManager.createNamedQuery("PuntosJugador.jugador", PuntosJugador.class)
                            .setParameter("nombre", j.getNombre())
                            .setParameter("jornada", jornada.getJornada())
                            .getSingleResult();

                    puntosGanados = puntosGanados + pJ.getValoracion();

                    pE.setPuntos(puntosGanados);
                    pE.getJugadores().add(pJ);
                } catch (NoResultException excep) { /* Si la consulta no devuelve un resultado sigo mirando */
                    PuntosJugador newPj = new PuntosJugador();
                    newPj.setJornada(jornada.getJornada());
                    newPj.setNombre(j.getNombre());
                    newPj.setPuntos(0);
                    newPj.setPosicion(j.getPosicion());
                    newPj.setValor_mercado(j.getValorMercado());
                    newPj.setValoracion(0);

                    entityManager.persist(newPj);
                    pE.getJugadores().add(newPj);
                }
            }

            e.setPuntos(e.getPuntos() + puntosGanados);
            entityManager.persist(pE);
            entityManager.flush();
        }

        List<PartidoACB> partidos = entityManager.createNamedQuery("PartidoACB.partidosJornada", PartidoACB.class)
                .setParameter("jornada", jornada.getJornada())
                .getResultList();

        for (PartidoACB p : partidos) {
            EquipoACB local = entityManager.createNamedQuery("EquipoACB.equipo", EquipoACB.class)
                    .setParameter("equipo", p.getLocal())
                    .getSingleResult();

            EquipoACB visitante = entityManager.createNamedQuery("EquipoACB.equipo", EquipoACB.class)
                    .setParameter("equipo", p.getVisitante())
                    .getSingleResult();

            int puntosLocal = p.getPuntosLocal();
            int puntosVisitante = p.getPuntosVisitante();
            local.setPuntosAFavor(local.getPuntosAFavor() + puntosLocal);
            local.setPuntosEnContra(local.getPuntosEnContra() + puntosVisitante);
            visitante.setPuntosAFavor(visitante.getPuntosAFavor() + puntosVisitante);
            visitante.setPuntosEnContra(visitante.getPuntosEnContra() + puntosLocal);

            if (puntosLocal > puntosVisitante) {
                local.setVictorias(local.getVictorias() + 1);
                visitante.setDerrotas(visitante.getDerrotas() + 1);
            } else {
                visitante.setVictorias(visitante.getVictorias() + 1);
                local.setDerrotas(local.getDerrotas() + 1);
            }

            local.setDiferencia(local.getPuntosAFavor() - local.getPuntosEnContra());
            visitante.setDiferencia(visitante.getPuntosAFavor() - visitante.getPuntosEnContra());

            entityManager.flush();
        }
    }

    @Transactional
    public void reiniciarBD() {
        // Vaciamos los mensajes enviados por los usuarios
        List<User> usuarios = entityManager.createNamedQuery("User.allUsers", User.class)
                .getResultList();

        for (User u : usuarios) {
            u.getSent().clear();
        }

        // Borramos los mensajes de la aplicación
        entityManager.createNamedQuery("Message.deleteMessages");

        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();
        jornada.setJornada(0);

        List<Equipo> equiposUsr = entityManager.createNamedQuery("Equipo.todosEquipos", Equipo.class)
                .getResultList();

        // Reiniciamos la información de todos los equipos de los usuarios
        for (Equipo e : equiposUsr) {
            e.setDinero(500000);
            e.setPuntos(0);
            e.getJugadores().clear();
        }

        // Reiniciamos las estadísticas de los jugadores
        List<JugadorACB> jugadores = entityManager.createNamedQuery("JugadorACB.jugadores", JugadorACB.class)
                .getResultList();

        for (JugadorACB j : jugadores) {
            j.setPartidosJugados(0);
            j.setPuntosTotales(0);
            j.setValorMercado(50000);
            j.setValoracionMedia(0);
            j.setValoracionTotal(0);
        }

        // Reiniciamos las estadísticas de los equipos
        List<EquipoACB> equipos = entityManager.createNamedQuery("EquipoACB.equipos", EquipoACB.class)
                .getResultList();

        for (EquipoACB e : equipos) {
            e.setVictorias(0);
            e.setDerrotas(0);
            e.setDiferencia(0);
            e.setPuntosAFavor(0);
            e.setPuntosEnContra(0);
        }

        // Eliminamos las puntuaciones de los equipos de las jornadas jugadas
        entityManager.createNamedQuery("PuntosEquipo.deletePuntosEquipo");

        entityManager.flush();
    }

    @Transactional
    public void procesarJornadaACB(MultipartFile file) throws IOException {
        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();

        String contenido = new String(file.getBytes());
        JSONArray jsonArray = new JSONArray(contenido);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject partido = jsonArray.getJSONObject(i);
            int jornadaPartido = partido.getInt("jornada");
            if (jornadaPartido >= jornada.getJornada()) {
                String local = partido.getString("local");
                int puntosLocal = partido.getInt("puntos_local");
                String visitante = partido.getString("visitante");
                int puntosVisitante = partido.getInt("puntos_visitante");

                PartidoACB nuevoPartido;
                try {
                    nuevoPartido = entityManager.createNamedQuery("PartidoACB.partido", PartidoACB.class)
                            .setParameter("local", local)
                            .setParameter("visitante", visitante)
                            .getSingleResult();

                } catch (NoResultException e) {
                    nuevoPartido = new PartidoACB();
                    nuevoPartido.setJornada(jornadaPartido);
                    nuevoPartido.setLocal(local);
                    nuevoPartido.setPuntosLocal(puntosLocal);
                    nuevoPartido.setPuntosVisitante(puntosVisitante);
                    nuevoPartido.setVisitante(visitante);

                    entityManager.persist(nuevoPartido);
                }

                nuevoPartido.setJornada(jornadaPartido);
                nuevoPartido.setLocal(local);
                nuevoPartido.setPuntosLocal(puntosLocal);
                nuevoPartido.setPuntosVisitante(puntosVisitante);
                nuevoPartido.setVisitante(visitante);

                entityManager.flush();
            }
        }

    }

    @Transactional
    public void procesarJugadorACB(MultipartFile file) throws IOException {
        String contenido = new String(file.getBytes());
        JSONArray jsonArray = new JSONArray(contenido);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject j = jsonArray.getJSONObject(i);
            String nombre = j.getString("nombre");

            try {
                JugadorACB jugador = entityManager.createNamedQuery("JugadorACB.jugador", JugadorACB.class)
                        .setParameter("nombre", nombre)
                        .getSingleResult();
            } catch (NoResultException e) {
                String pais = j.getString("pais");
                String posicion = j.getString("posicion");
                String equipo = j.getString("equipo");

                JugadorACB jugador = new JugadorACB();
                jugador.setNombre(nombre);
                jugador.setPais(pais);
                jugador.setPosicion(posicion);
                jugador.setEquipo(equipo);
                jugador.setPartidosJugados(0);
                jugador.setPuntosTotales(0);
                jugador.setValorMercado(50000);
                jugador.setValoracionMedia(0);
                jugador.setValoracionTotal(0);

                entityManager.persist(jugador);
            }
        }
    }

    @Transactional
    public void procesarPuntosJugador(MultipartFile file) throws IOException {
        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();

        String contenido = new String(file.getBytes());
        JSONArray jsonArray = new JSONArray(contenido);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject pJ = jsonArray.getJSONObject(i);
            int jornadaPartido = pJ.getInt("jornada");
            if (jornadaPartido >= jornada.getJornada()) {
                String nombre = pJ.getString("nombre");
                int puntos = pJ.getInt("puntos");
                int valoracion = pJ.getInt("valoracion");
                Double valor_mercado = pJ.getDouble("valor_mercado");

                JugadorACB jugador = entityManager.createNamedQuery("JugadorACB.jugador", JugadorACB.class)
                        .setParameter("nombre", nombre)
                        .getSingleResult();

                PuntosJugador nuevoPj;
                try {
                    nuevoPj = entityManager.createNamedQuery("PuntosJugador.jugador", PuntosJugador.class)
                            .setParameter("nombre", nombre)
                            .setParameter("jornada", jornadaPartido)
                            .getSingleResult();

                } catch (NoResultException e) {
                    nuevoPj = new PuntosJugador();
                    nuevoPj.setJornada(jornadaPartido);
                    nuevoPj.setNombre(nombre);
                    nuevoPj.setPosicion(jugador.getPosicion());
                    nuevoPj.setPuntos(puntos);
                    nuevoPj.setValor_mercado(valor_mercado);
                    nuevoPj.setValoracion(valoracion);

                    entityManager.persist(nuevoPj);
                }

                nuevoPj.setJornada(jornadaPartido);
                nuevoPj.setNombre(nombre);
                nuevoPj.setPosicion(jugador.getPosicion());
                nuevoPj.setPuntos(puntos);
                nuevoPj.setValor_mercado(valor_mercado);
                nuevoPj.setValoracion(valoracion);

                entityManager.flush();
            }
        }
    }

    @PostMapping("/banearUsuario")
    @Transactional
    public String banearUsuario(@RequestParam("idUsuario") Long idUsuario, Model model) {
        User u = entityManager.find(User.class, idUsuario);
        u.setEnabled(false);

        return index(model);
    }

    @PostMapping("/desbanearUsuario")
    @Transactional
    public String desbanearUsuario(@RequestParam("idUsuario") Long idUsuario, Model model) {
        User u = entityManager.find(User.class, idUsuario);
        u.setEnabled(true);

        return index(model);
    }

    @PostMapping("/eliminarMensaje")
    @Transactional
    public String eliminarMensaje(@RequestParam("idMensaje") Long idMensaje, Model model) {
        Message m = entityManager.find(Message.class, idMensaje);

        entityManager.remove(m);

        return index(model);
    }

    @PostMapping("/validarMensaje")
    @Transactional
    public String validarMensaje(@RequestParam("idMensaje") Long idMensaje, Model model) {

        Message m = entityManager.find(Message.class, idMensaje);

        m.setReported(false);

        return index(model);
    }

    @PostMapping("/avanzarJornada")
    @Transactional
    @ResponseBody
    public String avanzarJornada(Model model) {
        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();

        actualizarDatos();

        jornada.setJornada(jornada.getJornada() + 1);

        model.addAttribute("jornada", jornada.getJornada());

        return "{\"result\": \"jornada avanzada correctamente.\"}";
    }

    @PostMapping("/eliminarMensajes")
    @Transactional
    public String eliminarMensajes(@RequestParam("idUsuario") Long idUsuario, Model model) {
        User u = entityManager.find(User.class, idUsuario);

        u.getSent().clear();

        model.addAttribute("usuario", u);
        model.addAttribute("mensajes", u.getSent());

        return "mensajesUsuario";
    }

    @PostMapping("/anadirJornadas")
    @Transactional
    public String añadirJornadas(@RequestParam("bdfiles") MultipartFile[] files, Model model) {
        // Damos por hecho que se han insertado los archivos que queremos y que tiene
        // datos

        // Forzamos a ejecutar procesarJugadorACB por si hay algún nuevo jugador y
        // evitar posibles errores
        for (MultipartFile file : files) {
            String nombreArchivo = file.getOriginalFilename();
            try {
                if (nombreArchivo.equals("jugador_acb.json")) {
                    procesarJugadorACB(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Luego procesamos los demás archivos
        for (MultipartFile file : files) {
            String nombreArchivo = file.getOriginalFilename();
            try {
                if (nombreArchivo.equals("jornada_acb.json")) {
                    procesarJornadaACB(file);
                } else if (nombreArchivo.equals("puntos_jugador.json")) {
                    procesarPuntosJugador(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return index(model);
    }

    @PostMapping("/reiniciarApp")
    @Transactional
    public String reiniciarApp(Model model) {
        reiniciarBD();
        return index(model);
    }

}
