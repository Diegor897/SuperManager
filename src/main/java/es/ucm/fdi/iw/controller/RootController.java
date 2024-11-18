package es.ucm.fdi.iw.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.EquipoACB;
import es.ucm.fdi.iw.model.Jornada;
import es.ucm.fdi.iw.model.PartidoACB;
import es.ucm.fdi.iw.model.JugadorACB;
import es.ucm.fdi.iw.model.Liga;
import es.ucm.fdi.iw.model.PuntosEquipo;
import es.ucm.fdi.iw.model.PuntosJugador;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.User.Role;
import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.Transferable;

import java.time.LocalDateTime;

/**
 * Non-authenticated requests only.
 */
@Controller
public class RootController {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Logger log = LogManager.getLogger(RootController.class);

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @GetMapping("/registro")
    public String registro(Model model) {
        return "registro";
    }

    /**
     * Register a new user
     */
    @PostMapping("/registro")
    @Transactional
    public String createuser(@RequestParam String username, @RequestParam String password,
            HttpSession session, Model model) throws IOException {

        List<User> existentes = entityManager.createNamedQuery("User.byUsername", User.class)
                .setParameter("username", username)
                .getResultList();

        if (!existentes.isEmpty()) {
            model.addAttribute("error", "Usuario ya registrado");
            return "registro";
        }

        User candidato = new User();
        candidato.setUsername(username);
        candidato.setPassword(passwordEncoder.encode(password));
        candidato.setEnabled(true);
        candidato.setRoles(Role.USER.name());
        entityManager.persist(candidato);
        entityManager.flush();

        return "redirect:login";
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User u = (User) session.getAttribute("u");
        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();
        // if (u != null) {
        // model.addAttribute("nombreUsuario", u.getUsername());
        // } else {
        // model.addAttribute("nombreUsuario", null);
        // }

        model.addAttribute("nombreUsuario", u.getUsername());

        List<EquipoACB> equipos = entityManager.createNamedQuery("EquipoACB.equipos", EquipoACB.class)
                .getResultList();

        // Define el comparador personalizado
        Comparator<EquipoACB> comparador = Comparator
                .comparingInt((EquipoACB equipo) -> -equipo.getVictorias()) // Orden descendente por victorias
                .thenComparingInt(EquipoACB::getDerrotas) // Orden ascendente por derrotas
                .thenComparingInt((EquipoACB equipo) -> -equipo.getPuntosAFavor()) // Orden descendente por puntos a
                                                                                   // favor
                .thenComparingInt(EquipoACB::getPuntosEnContra) // Orden ascendente por puntos en contra
                .thenComparingInt((EquipoACB equipo) -> -equipo.getDiferencia()); // Orden descendente por diferencia

        // Ordena la lista de equipos utilizando el comparador
        Collections.sort(equipos, comparador);

        model.addAttribute("equipos", equipos);

        List<PartidoACB> partidosant = entityManager.createNamedQuery("PartidoACB.partidosJornada", PartidoACB.class)
                .setParameter("jornada", jornada.getJornada() - 1)
                .getResultList();

        List<PartidoACB> partidos = entityManager.createNamedQuery("PartidoACB.partidosJornada", PartidoACB.class)
                .setParameter("jornada", jornada.getJornada())
                .getResultList();

        model.addAttribute("partidos", partidos);
        model.addAttribute("jornada", jornada.getJornada());
        model.addAttribute("partidosant", partidosant);

        return "index";
    }

    /**
     * Devuelve la información de las ligas a las que está unido un usuario
     */
    @GetMapping("/clasificacion")
    public String getLigas(HttpSession session, Model model) {
        User owner = (User) session.getAttribute("u");
        List<Equipo> misEquipos = entityManager.createNamedQuery("Equipo.misEquipos", Equipo.class)
                .setParameter("owner", owner)
                .getResultList();
        Map<String, Integer> posiciones = new HashMap<>();
        model.addAttribute("equipos", misEquipos);

        for (Equipo e : misEquipos) {
            Liga liga = e.getLiga();
            if (liga != null) {
                List<Equipo> equipos = entityManager.createNamedQuery("Equipo.byliga", Equipo.class)
                        .setParameter("liga", liga)
                        .getResultList();

                equipos.sort(Comparator.comparingInt(Equipo::getPuntos).reversed());

                int posicion = equipos.indexOf(e) + 1; // Sumamos 1 para que la posición comience desde 1

                // Guardar la posición en el mapa
                posiciones.put(e.getTeamname(), posicion);
            }
        }

        model.addAttribute("posiciones", posiciones);

        return "clasificacion";
    }

    /**
     * Devuelve la información de las ligas a las que está unido un usuario
     */
    @GetMapping("/ligas")
    public String getLigas(@RequestParam String nombreEquipo, HttpSession session, Model model) {
        User owner = (User) session.getAttribute("u");
        List<Equipo> misEquipos = entityManager.createNamedQuery("Equipo.misEquipos", Equipo.class)
                .setParameter("owner", owner)
                .getResultList();

        List<Liga> ligasDisponibles = entityManager.createNamedQuery("Liga.allLigas", Liga.class)
                .getResultList();

        // Obtener una lista de las ligas a las que hay equipos unidos
        List<Liga> ligasConEquipos = misEquipos.stream()
                .map(Equipo::getLiga)
                .collect(Collectors.toList());

        // Filtrar las ligas disponibles para excluir aquellas a las que hay equipos
        // unidos
        List<Liga> ligasSinEquipos = ligasDisponibles.stream()
                .filter(liga -> !ligasConEquipos.contains(liga))
                .collect(Collectors.toList());

        model.addAttribute("nombreEquipo", nombreEquipo);
        model.addAttribute("ligas", ligasSinEquipos);
        return "ligas";
    }

    /**
     * Devuelve la información de los equipos de un usuario
     */
    @GetMapping("/equipos")
    public String getEquipos(HttpSession session, Model model) {
        User owner = (User) session.getAttribute("u");
        List<Equipo> misEquipos = entityManager.createNamedQuery("Equipo.misEquipos", Equipo.class)
                .setParameter("owner", owner)
                .getResultList();

        model.addAttribute("misEquipos", misEquipos);

        Map<String, Integer> posiciones = new HashMap<>();

        for (Equipo e : misEquipos) {
            Liga liga = e.getLiga();
            if (liga != null) {
                List<Equipo> equipos = entityManager.createNamedQuery("Equipo.byliga", Equipo.class)
                        .setParameter("liga", liga)
                        .getResultList();

                equipos.sort(Comparator.comparingInt(Equipo::getPuntos).reversed());

                int posicion = equipos.indexOf(e) + 1; // Sumamos 1 para que la posición comience desde 1

                // Guardar la posición en el mapa
                posiciones.put(e.getTeamname(), posicion);
            }
        }

        model.addAttribute("posiciones", posiciones);

        return "equipos";
    }

    @GetMapping("/mercado")
    public String getMercado(Model model) {
        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();
        int jornadaActual = jornada.getJornada();

        List<JugadorACB> jugadores = new ArrayList<>();

        jugadores = entityManager.createNamedQuery("JugadorACB.jugadores", JugadorACB.class)
                .getResultList();

        jugadores.sort(Comparator.comparing(JugadorACB::getNombre));

        Map<String, List<Integer>> ultimosPartidos = new HashMap<String, List<Integer>>();
        for (JugadorACB j : jugadores) {
            List<Integer> listUltPartidos = entityManager
                    .createNamedQuery("PuntosJugador.ultimosPartidos", Integer.class)
                    .setParameter("nombre", j.getNombre())
                    .setParameter("jornada", jornadaActual)
                    .setMaxResults(3)
                    .getResultList();

            ultimosPartidos.put(j.getNombre(), listUltPartidos);
        }

        model.addAttribute("jugadores", jugadores);
        model.addAttribute("jornada", jornadaActual);
        model.addAttribute("ultimosPartidos", ultimosPartidos);

        return "mercado";
    }

    @PostMapping("/mercado")
    public String postMercado(@RequestParam(name = "buscar", required = false) String buscar,
            @RequestParam(name = "posicion", required = false) String posicion, Model model) {
        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();
        int jornadaActual = jornada.getJornada();

        List<JugadorACB> jugadores = new ArrayList<>();

        if (posicion != null) {
            if (posicion.equals("Base")) {
                jugadores = entityManager.createNamedQuery("JugadorACB.bases", JugadorACB.class)
                        .getResultList();
            } else if (posicion.equals("Alero")) {
                jugadores = entityManager.createNamedQuery("JugadorACB.aleros", JugadorACB.class)
                        .getResultList();
            } else {
                jugadores = entityManager.createNamedQuery("JugadorACB.pivots", JugadorACB.class)
                        .getResultList();
            }
        } else {
            jugadores = entityManager.createNamedQuery("JugadorACB.nombreSimilar", JugadorACB.class)
                    .setParameter("nombreSimilar", "%" + buscar + "%")
                    .getResultList();
        }

        Map<String, List<Integer>> ultimosPartidos = new HashMap<String, List<Integer>>();
        for (JugadorACB j : jugadores) {
            List<Integer> listUltPartidos = entityManager
                    .createNamedQuery("PuntosJugador.ultimosPartidos", Integer.class)
                    .setParameter("nombre", j.getNombre())
                    .setParameter("jornada", jornadaActual)
                    .setMaxResults(3)
                    .getResultList();

            ultimosPartidos.put(j.getNombre(), listUltPartidos);
        }

        jugadores.sort(Comparator.comparing(JugadorACB::getNombre));
        model.addAttribute("jugadores", jugadores);
        model.addAttribute("jornada", jornadaActual);
        model.addAttribute("ultimosPartidos", ultimosPartidos);

        return "mercado";
    }

    @GetMapping("/actualidad")
    public String getActualidad(HttpSession session, Model model) {
        User owner = (User) session.getAttribute("u");

        List<Equipo> misEquipos = entityManager.createNamedQuery("Equipo.misEquipos", Equipo.class)
                .setParameter("owner", owner)
                .getResultList();

        // Guardar las ligas en las que se ha unido el usuario
        List<Liga> ligasUsuario = new ArrayList<>();
        for (Equipo equipo : misEquipos) {
            Liga liga = equipo.getLiga();
            if (liga != null && !ligasUsuario.contains(liga)) {
                ligasUsuario.add(liga);
            }
        }

        model.addAttribute("ligas", ligasUsuario);
        return "actualidad";
    }

    @GetMapping("/jugador/{idJugador}")
    public String getJugador(@PathVariable long idJugador, Model model) {
        JugadorACB jugador = entityManager.find(JugadorACB.class, idJugador);

        Jornada jornadaActual = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();

        List<Integer> listUltPartidos = entityManager.createNamedQuery("PuntosJugador.ultimosPartidos", Integer.class)
                .setParameter("nombre", jugador.getNombre())
                .setParameter("jornada", jornadaActual.getJornada())
                .setMaxResults(3)
                .getResultList();

        model.addAttribute("jugador", jugador);
        model.addAttribute("ultimosPartidos", listUltPartidos);
        return "jugador";
    }

    @GetMapping("/liga/{idLiga}")
    public String getLiga(@PathVariable long idLiga, Model model) {
        Liga liga = entityManager.find(Liga.class, idLiga);

        List<Equipo> equipos = entityManager.createNamedQuery("Equipo.byliga", Equipo.class)
                .setParameter("liga", liga)
                .getResultList();

        Jornada jornadaActual = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();

        Map<String, List<Integer>> ultimasJornadas = new HashMap<String, List<Integer>>();
        for (Equipo e : equipos) {
            List<Integer> listUltJornadas = entityManager
                    .createNamedQuery("PuntosEquipo.ultimasJornadas", Integer.class)
                    .setParameter("equipo", e)
                    .setParameter("jornada", jornadaActual.getJornada())
                    .setMaxResults(3)
                    .getResultList();

            ultimasJornadas.put(e.getTeamname(), listUltJornadas);
        }

        equipos.sort(Comparator.comparingInt(Equipo::getPuntos).reversed());

        model.addAttribute("equipos", equipos);
        model.addAttribute("nombreLiga", liga.getNombreLiga());
        model.addAttribute("ultimasJornadas", ultimasJornadas);

        return "liga";
    }

    @GetMapping("/foro/{idLiga}")
    public String getForo(@PathVariable long idLiga, HttpSession session, Model model) {
        Liga liga = entityManager.find(Liga.class, idLiga);

        model.addAttribute("liga", liga);
        return "foro";
    }

    @PutMapping("/foro/{idLiga}")
    @ResponseBody
    @Transactional
    public String reportMessage(@PathVariable long idLiga, @RequestBody JsonNode o, HttpSession session, Model model) {
        Long idMensaje = o.get("idMensaje").asLong();
        Message m = entityManager.find(Message.class, idMensaje);

        m.setReported(true);
        entityManager.flush();

        Liga liga = entityManager.find(Liga.class, idLiga);

        model.addAttribute("liga", liga);

        return "{\"result\": \"message reported.\"}";
    }

    @GetMapping("/mensajes/{idLiga}")
    @ResponseBody
    public List<Message.Transfer> getForoApi(@PathVariable long idLiga) {
        Liga liga = entityManager.find(Liga.class, idLiga);

        List<Message> mensajes = liga.getReceived();

        return mensajes.stream().map(Transferable::toTransfer).collect(Collectors.toList());
    }

    @PostMapping("/foro/{idLiga}")
    @ResponseBody
    @Transactional
    public String postMensaje(@PathVariable long idLiga, @RequestBody JsonNode o, HttpSession session)
            throws JsonProcessingException {

        String text = o.get("message").asText();
        User sender = (User) session.getAttribute("u");
        sender = entityManager.find(User.class, sender.getId());
        Liga liga = entityManager.find(Liga.class, idLiga);

        // Crear un nuevo mensaje
        Message m = new Message();
        m.setSender(sender);
        m.setRecipient(liga);
        m.setText(text);
        m.setDateSent(LocalDateTime.now());
        m.setReported(false);
        liga.getReceived().add(m);
        m.toTransfer();

        // Guardo el mensaje en la lista de mensajes del usuario
        sender.getSent().add(m);

        entityManager.persist(m);
        entityManager.flush();

        ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writeValueAsString(m.toTransfer());

        log.info("Sending a message to {} with contents '{}'", "/topic/" + liga.getId(), json);
        messagingTemplate.convertAndSend("/topic/" + liga.getId(), json);
        return "{\"result\": \"message sent.\"}";
    }

    @GetMapping("/crearliga")
    public String getCrearLiga() {
        return "crearliga";
    }

    /**
     * Crea una nueva liga
     */
    @PostMapping("/crearliga")
    @Transactional
    public String createLiga(@RequestParam String nombreliga, @RequestParam String password,
            HttpSession session, Model model) throws IOException {

        List<Liga> existentes = entityManager.createNamedQuery("Liga.bynombreliga", Liga.class)
                .setParameter("nombreliga", nombreliga)
                .getResultList();

        if (!existentes.isEmpty()) {
            model.addAttribute("error", "Nombre de liga ya existente");
            return "crearliga";
        }

        Liga candidato = new Liga();
        candidato.setNombreLiga(nombreliga);
        candidato.setContrasena(password);
        User u = (User) session.getAttribute("u");
        candidato.setCreador(u);
        entityManager.persist(candidato);
        entityManager.flush();

        return "redirect:clasificacion";
    }

    @GetMapping("/crearequipo")
    public String getCrearEquipo() {
        return "crearequipo";
    }

    /**
     * Crea una nuevo equipo
     */
    @PostMapping("/crearequipo")
    @Transactional
    public String creaEquipo(@RequestParam String nombreequipo,
            HttpSession session, Model model) throws IOException {

        List<Equipo> existentes = entityManager.createNamedQuery("Equipo.bynombreequipo", Equipo.class)
                .setParameter("nombreequipo", nombreequipo)
                .getResultList();

        if (!existentes.isEmpty()) {
            model.addAttribute("error", "Nombre de equipo ya existente");
            return "crearequipo";
        }

        Equipo candidato = new Equipo();
        User u = (User) session.getAttribute("u");
        candidato.setOwner(u);
        candidato.setTeamname(nombreequipo);
        candidato.setDinero(500000);
        candidato.setPuntos(0);
        entityManager.persist(candidato);
        entityManager.flush();

        return "redirect:equipos";
    }

    @GetMapping("/unirseliga")
    public String getUnirseLiga(@RequestParam String nombreEquipo, @RequestParam String nombreLiga, Model model) {
        model.addAttribute("nombreEquipo", nombreEquipo);
        model.addAttribute("nombreLiga", nombreLiga);
        return "unirseliga";
    }

    @PostMapping("/unirseliga")
    @Transactional
    public String unirLiga(@RequestParam String nombreLiga, @RequestParam String nombreEquipo,
            @RequestParam String password, HttpSession session, Model model) {
        Liga liga = entityManager.createNamedQuery("Liga.bynombreliga", Liga.class)
                .setParameter("nombreliga", nombreLiga)
                .getSingleResult();

        if (!liga.getContrasena().equals(password)) {
            model.addAttribute("error", "Contraseña errónea");
        } else {
            Equipo equipo = entityManager.createNamedQuery("Equipo.bynombreequipo", Equipo.class)
                    .setParameter("nombreequipo", nombreEquipo)
                    .getSingleResult();
            equipo.setLiga(liga);
            entityManager.persist(equipo);
            entityManager.flush();

            List<String> misLigas = (List<String>) session.getAttribute("misLigas");
            misLigas.add("/topic/" + liga.getId());
            session.setAttribute("misLigas", misLigas);

            return "redirect:clasificacion";
        }

        return "redirect:unirseliga" + "?nombreEquipo=" + nombreEquipo + "&nombreLiga=" + nombreLiga;
    }

    @GetMapping("/MiEquipo/{idEquipo}")
    public String getMiEquipo(@PathVariable Long idEquipo, HttpSession session, Model model) {
        User u = (User) session.getAttribute("u");
        Equipo equipo = entityManager.find(Equipo.class, idEquipo);

        List<JugadorACB> jugadores = equipo.getJugadores();

        List<JugadorACB> bases = new ArrayList<>();
        List<JugadorACB> aleros = new ArrayList<>();
        List<JugadorACB> pivots = new ArrayList<>();
        for (JugadorACB e : jugadores) {
            if (e.getPosicion().equals("Base"))
                bases.add(e);
            else if (e.getPosicion().equals("Alero"))
                aleros.add(e);
            else
                pivots.add(e);
        }

        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();

        Map<String, List<Integer>> ultimosPartidos = new HashMap<String, List<Integer>>();
        for (JugadorACB j : jugadores) {
            List<Integer> listUltPartidos = entityManager
                    .createNamedQuery("PuntosJugador.ultimosPartidos", Integer.class)
                    .setParameter("nombre", j.getNombre())
                    .setParameter("jornada", jornada.getJornada())
                    .setMaxResults(3)
                    .getResultList();

            ultimosPartidos.put(j.getNombre(), listUltPartidos);
        }

        model.addAttribute("equipo", equipo);
        model.addAttribute("idequipo", idEquipo);
        model.addAttribute("bases", bases);
        model.addAttribute("aleros", aleros);
        model.addAttribute("pivots", pivots);
        model.addAttribute("jornada", jornada.getJornada());
        model.addAttribute("ultimosPartidos", ultimosPartidos);
        model.addAttribute("u", u);

        // Si hay error al fichar muestro el error y lo quito de la sesión
        model.addAttribute("error", session.getAttribute("error"));
        session.removeAttribute("error");

        return "MiEquipo";
    }

    @PostMapping("/MiEquipo/{idEquipo}")
    @Transactional
    public String ficharJugador(@PathVariable Long idEquipo, @RequestParam String nombreJugador,
            @RequestParam String formType, HttpSession session, Model model) {
        User u = (User) session.getAttribute("u");
        Equipo equipo = entityManager.createNamedQuery("Equipo.miEquipo", Equipo.class)
                .setParameter("idEquipo", idEquipo)
                .setParameter("owner", u)
                .getSingleResult();

        JugadorACB jugador = entityManager.createNamedQuery("JugadorACB.jugador", JugadorACB.class)
                .setParameter("nombre", nombreJugador)
                .getSingleResult();

        if (formType.equals("ficharJugador")) {
            if (equipo.getDinero() < jugador.getValorMercado()) {
                // Añado a la sesión el error para poder acceder fácilmente
                session.setAttribute("error", "Dinero insuficiente para el fichaje");
            } else {
                equipo.getJugadores().add(jugador);
                equipo.setDinero(equipo.getDinero() - jugador.getValorMercado());
            }
        } else {
            equipo.getJugadores().remove(jugador);
            equipo.setDinero(equipo.getDinero() + jugador.getValorMercado());
        }

        entityManager.flush();

        return "redirect:/MiEquipo/" + idEquipo;
    }

    @GetMapping("/fichar")
    public String getFicharJug() {
        return "fichar";
    }

    @PostMapping("/fichar")
    public String ficharJugadores(@RequestParam(name = "buscar", required = false) String buscar,
            @RequestParam("formType") String formType, @RequestParam("idequipo") Long idequipo, Model model) {
        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();

        List<JugadorACB> jugadores = new ArrayList<>();
        List<JugadorACB> jugadoresFichados = entityManager.find(Equipo.class, idequipo)
                .getJugadores();

        if (buscar != null) {
            if (formType.equals("ficharBases")) {
                jugadores = entityManager.createNamedQuery("JugadorACB.nombreSimilarBases", JugadorACB.class)
                        .setParameter("nombreSimilar", "%" + buscar + "%")
                        .getResultList();
                jugadores.removeAll(jugadoresFichados);
            } else if (formType.equals("ficharAleros")) {
                jugadores = entityManager.createNamedQuery("JugadorACB.nombreSimilarAleros", JugadorACB.class)
                        .setParameter("nombreSimilar", "%" + buscar + "%")
                        .getResultList();
                jugadores.removeAll(jugadoresFichados);
            } else {
                jugadores = entityManager.createNamedQuery("JugadorACB.nombreSimilarPivots", JugadorACB.class)
                        .setParameter("nombreSimilar", "%" + buscar + "%")
                        .getResultList();
                jugadores.removeAll(jugadoresFichados);
            }
        } else {
            if (formType.equals("ficharBases")) {
                jugadores = entityManager.createNamedQuery("JugadorACB.bases", JugadorACB.class)
                        .getResultList();
                jugadores.removeAll(jugadoresFichados);
            } else if (formType.equals("ficharAleros")) {
                jugadores = entityManager.createNamedQuery("JugadorACB.aleros", JugadorACB.class)
                        .getResultList();
                jugadores.removeAll(jugadoresFichados);
            } else {
                jugadores = entityManager.createNamedQuery("JugadorACB.pivots", JugadorACB.class)
                        .getResultList();
                jugadores.removeAll(jugadoresFichados);
            }
        }

        jugadores.sort(Comparator.comparing(JugadorACB::getNombre));

        Map<String, List<Integer>> ultimosPartidos = new HashMap<String, List<Integer>>();
        for (JugadorACB j : jugadores) {
            List<Integer> listUltPartidos = entityManager
                    .createNamedQuery("PuntosJugador.ultimosPartidos", Integer.class)
                    .setParameter("nombre", j.getNombre())
                    .setParameter("jornada", jornada.getJornada())
                    .setMaxResults(3)
                    .getResultList();

            ultimosPartidos.put(j.getNombre(), listUltPartidos);
        }

        model.addAttribute("idequipo", idequipo);
        model.addAttribute("jugadores", jugadores);
        model.addAttribute("formType", formType);
        model.addAttribute("jornada", jornada.getJornada());
        model.addAttribute("ultimosPartidos", ultimosPartidos);

        return "fichar";
    }

    @GetMapping("/vender")
    public String getVenderJug() {
        return "vender";
    }

    @PostMapping("/vender")
    public String venderJugadores(@RequestParam("formType") String formType, @RequestParam("idequipo") Long idequipo,
            @RequestParam("jugador") String nombreJugador, Model model) {
        JugadorACB jugador = entityManager.createNamedQuery("JugadorACB.jugador", JugadorACB.class)
                .setParameter("nombre", nombreJugador)
                .getSingleResult();

        Jornada jornada = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();

        List<Integer> listUltPartidos = entityManager.createNamedQuery("PuntosJugador.ultimosPartidos", Integer.class)
                .setParameter("nombre", jugador.getNombre())
                .setParameter("jornada", jornada.getJornada())
                .setMaxResults(3)
                .getResultList();

        model.addAttribute("idequipo", idequipo);
        model.addAttribute("jugador", jugador);
        model.addAttribute("ultimosPartidos", listUltPartidos);

        return "vender";
    }

    @GetMapping("/equipoJornada")
    public String getEquipoJornada() {
        return "equipoJornada";
    }

    @PostMapping("/equipoJornada")
    public String postEquipoJornada(@RequestParam("jornadaJugada") int jornada, @RequestParam("idequipo") Long idEquipo,
            Model model) {
        Jornada jornadaActual = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();

        Equipo equipo = entityManager.find(Equipo.class, idEquipo);
        PuntosEquipo pE = entityManager.createNamedQuery("PuntosEquipo.equipo", PuntosEquipo.class)
                .setParameter("equipo", equipo)
                .setParameter("jornada", jornada)
                .getSingleResult();

        List<PuntosJugador> jugadores = pE.getJugadores();

        List<PuntosJugador> bases = new ArrayList<>();
        List<PuntosJugador> aleros = new ArrayList<>();
        List<PuntosJugador> pivots = new ArrayList<>();
        for (PuntosJugador e : jugadores) {
            if (e.getPosicion().equals("Base"))
                bases.add(e);
            else if (e.getPosicion().equals("Alero"))
                aleros.add(e);
            else
                pivots.add(e);
        }

        model.addAttribute("equipo", equipo);
        model.addAttribute("equipoJornada", pE);
        model.addAttribute("idequipo", idEquipo);
        model.addAttribute("bases", bases);
        model.addAttribute("aleros", aleros);
        model.addAttribute("pivots", pivots);
        model.addAttribute("jornada", jornadaActual.getJornada());

        return "equipoJornada";
    }

    @GetMapping("/perfilPublico/{idUsuario}")
    public String perfilUsuario(@PathVariable Long idUsuario, HttpSession session, Model model) {
        Jornada jornadaActual = entityManager.createNamedQuery("Jornada.getJornada", Jornada.class).getSingleResult();
        User u = (User) session.getAttribute("u");
        // Si un usuario pulsa en su enlace se le redirige a su propio perfil
        if (idUsuario == u.getId()) {
            model.addAttribute("user", u);
            return "user";
        }

        u = entityManager.find(User.class, idUsuario);
        List<Equipo> equipos = entityManager.createNamedQuery("Equipo.misEquipos", Equipo.class)
                .setParameter("owner", u)
                .getResultList();

        Map<String, Integer> posiciones = new HashMap<>();

        for (Equipo e : equipos) {
            Liga liga = e.getLiga();
            if (liga != null) {
                List<Equipo> equiposLiga = entityManager.createNamedQuery("Equipo.byliga", Equipo.class)
                        .setParameter("liga", liga)
                        .getResultList();

                equiposLiga.sort(Comparator.comparingInt(Equipo::getPuntos).reversed());

                int posicion = equiposLiga.indexOf(e) + 1; // Sumamos 1 para que la posición comience desde 1

                // Guardar la posición en el mapa
                posiciones.put(e.getTeamname(), posicion);
            }
        }

        Map<String, List<Integer>> ultimasJornadas = new HashMap<String, List<Integer>>();
        for (Equipo e : equipos) {
            List<Integer> listUltJornadas = entityManager
                    .createNamedQuery("PuntosEquipo.ultimasJornadas", Integer.class)
                    .setParameter("equipo", e)
                    .setParameter("jornada", jornadaActual.getJornada())
                    .setMaxResults(3)
                    .getResultList();

            ultimasJornadas.put(e.getTeamname(), listUltJornadas);
        }

        model.addAttribute("usuario", u);
        model.addAttribute("equipos", equipos);
        model.addAttribute("posiciones", posiciones);
        model.addAttribute("ultimasJornadas", ultimasJornadas);

        return "perfilPublico";
    }

    @GetMapping("/equipo/{id}")
    public String verPartidosEquipo(@PathVariable long id, Model model) {

        // obtengo objeto equipo con sus atributos
        EquipoACB equipo = entityManager.find(EquipoACB.class, id);

        String nombreEquipo = equipo.getNombreEquipo();

        // busco en la tabla de PartidoACB todas las filas que incluyen a equipo como
        // local o vistante (que participe) y las guardo en una lista
        List<PartidoACB> partidos = entityManager
                .createQuery("SELECT p FROM PartidoACB p WHERE p.local = :equipo OR p.visitante = :equipo",
                        PartidoACB.class)
                .setParameter("equipo", nombreEquipo)
                .getResultList();

        model.addAttribute("equipo", equipo);
        model.addAttribute("partidos", partidos);

        return "partidosEquipo";
    }

}
