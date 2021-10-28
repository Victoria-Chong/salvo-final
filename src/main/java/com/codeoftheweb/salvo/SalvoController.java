package com.codeoftheweb.salvo;

import com.codeoftheweb.salvo.Class.*;
import com.codeoftheweb.salvo.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SalvoController {
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    @Autowired
    private ShipRepository shipRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private SalvoRepository salvoRepository;
    @Autowired
    private ScoreRepository scoreRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    @RequestMapping("/games")
    public Map<String, Object> getControllerDTO(Authentication authentication) {
        Map<String, Object> dto = new LinkedHashMap<>();
        if (isGuest(authentication)){
            dto.put("player", "Guest");
        }
        else {
            dto.put("player", playerRepository
                    .findByUserName(authentication.getName())
                    .makePlayerDTO());
        }
        dto.put("games", gameRepository.findAll()
                .stream()
                .map(x -> x.makeGameDTO())
                .collect(Collectors.toList()));
        return dto;
    }

    @RequestMapping("/game_view/{nn}")
    public ResponseEntity<Map> findGamePlayer(@PathVariable Long nn, Authentication authentication) {
        GamePlayer gamePlayer = gamePlayerRepository.findById(nn).get();
        if (playerRepository.findByUserName(authentication.getName())
                .getGamePlayer()
                .stream()
                .anyMatch(x->x.getId().equals(nn))){
            return new ResponseEntity<>(gamePlayer.makeGameViewDTO(), HttpStatus.ACCEPTED);
        }
        else{
            return new ResponseEntity<>(makeMap("No se puede ver", 0), HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(path = "/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createUser(@RequestParam String email, @RequestParam String password) {
        if (email.isEmpty()) {
            return new ResponseEntity<>(makeMap("error", "No name"), HttpStatus.FORBIDDEN);
        }
        Player player = playerRepository.findByUserName(email);
        if (player != null) {
            return new ResponseEntity<>(makeMap("error", "Username already exists"), HttpStatus.CONFLICT);
        }
        Player newPlayer = playerRepository.save(new Player(email, passwordEncoder.encode(password)));
        return new ResponseEntity<>(makeMap("id", newPlayer.getId()), HttpStatus.CREATED);
    }

    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    @PostMapping(path = "/games")
    public ResponseEntity<Map>createGame(Authentication authentication){
        LocalDateTime Tiempo = LocalDateTime.now();
        Game newgame = gameRepository.save(new Game(Tiempo));
        GamePlayer newGamePlayer = gamePlayerRepository.save(new GamePlayer(this.playerRepository
                .findByUserName(authentication.getName()),newgame,LocalDateTime.now()));
        return new ResponseEntity<>(makeMap("gpid", newGamePlayer.getId()), HttpStatus.ACCEPTED);
    }

    @PostMapping("/game/{game}/players")
    public ResponseEntity<Map<String, Object>>joinGameButton(@PathVariable Long game, Authentication authentication){
        if (playerRepository.findByUserName(authentication.getName()) == null){
            return new ResponseEntity<>(makeMap("error", "No exist email"),HttpStatus.UNAUTHORIZED);
        }
        if (gameRepository.findById(game).isPresent()){
            if (gameRepository.getById(game).getId() == null){
                return new ResponseEntity<>(makeMap("error", "No exist"), HttpStatus.FORBIDDEN);
        }}
        if (gameRepository.getById(game).getGamePlayer().size() >= 2){
            return new ResponseEntity<>(makeMap("error", "Supero los jugadores"), HttpStatus.FORBIDDEN);
        }
        GamePlayer newGamePlayer = gamePlayerRepository.save(new GamePlayer(this.playerRepository
                .findByUserName(authentication.getName()),gameRepository.getById(game), LocalDateTime.now()));
        return new ResponseEntity<>(makeMap("gpid", newGamePlayer.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/games/players/{gamePlayerId}/ships", method = RequestMethod.POST)
    public ResponseEntity<Map> placeShips(@PathVariable Long gamePlayerId, @RequestBody List<Ship> locationShip, Authentication authentication){
        if (playerRepository.findByUserName(authentication.getName()) == null){
            return new ResponseEntity<>(makeMap("error", "No logged"), HttpStatus.UNAUTHORIZED);
        }
        if (gamePlayerRepository.findAll().stream().noneMatch(x -> x.getId().equals(gamePlayerId))){
            return new ResponseEntity<>(makeMap("error", "No hay jugadores"), HttpStatus.FORBIDDEN);
        }
        if (playerRepository.findByUserName(authentication.getName()).getGamePlayer().stream().noneMatch(x -> x.getId().equals(gamePlayerId))){
            return new ResponseEntity<>(makeMap("error", "No es un jugador"), HttpStatus.FORBIDDEN);
        }
        locationShip.forEach(x -> x.setGamePlayer(gamePlayerRepository.getById(gamePlayerId)));
        locationShip.forEach(x -> shipRepository.save(x));
        return new ResponseEntity<>(makeMap("OK", "Barcos colocados"), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/games/players/{gamePlayerId}/ships", method = RequestMethod.GET)
    public ResponseEntity<Map> placeShips(@PathVariable Long gamePlayerId){
        if (gamePlayerRepository.findById(gamePlayerId).isPresent()){
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("ship", gamePlayerRepository.findById(gamePlayerId)
                    .get()
                    .getShips()
                    .stream()
                    .map(x -> x.makeShipDTO())
                    .collect(Collectors.toList()));
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        }
        return new ResponseEntity<>(makeMap("error", "No exist"), HttpStatus.FORBIDDEN);
    }

    @RequestMapping(value = "/games/players/{gamePlayerId}/salvoes", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> placeSalvos(@PathVariable Long gamePlayerId, @RequestBody Salvo salvoLocations, Authentication authentication){
        List<Integer> gato = new ArrayList<>();
        if (playerRepository.findByUserName(authentication.getName()) == null){
            return new ResponseEntity<>(makeMap("error", "no logged"), HttpStatus.UNAUTHORIZED);
        }
        if (gamePlayerRepository.findAll().stream().noneMatch(x -> x.getId().equals(gamePlayerId))){
            return new ResponseEntity<>(makeMap("error", "No hay jugadores"), HttpStatus.FORBIDDEN);
        }
        if (playerRepository.findByUserName(authentication.getName()).getGamePlayer().stream().noneMatch(x -> x.getId().equals(gamePlayerId))){
            return new ResponseEntity<>(makeMap("error", "No es un jugador"), HttpStatus.FORBIDDEN);
        }
        if (gamePlayerRepository.getById(gamePlayerId).getGame().getPlayer().size() < 2){
            return new ResponseEntity<>(makeMap("error", "Espere al proximo jugador"), HttpStatus.FORBIDDEN);
        }
        salvoLocations.setGamePlayer(gamePlayerRepository.getById(gamePlayerId));
        GamePlayer playerId = gamePlayerRepository.getById(gamePlayerId);
        GamePlayer player2Id = gamePlayerRepository.getById(gamePlayerId).getGame()
                .getGamePlayer()
                .stream()
                .filter(x -> !x.getId().equals(gamePlayerId))
                .findFirst()
                .get();
        int player1 = playerId.getSalvo().size();
        int player2 = player2Id.getSalvo().size();
        if (gamePlayerRepository.getById(gamePlayerId).getSalvo().size() <= 0){
            salvoLocations.setTurn(1);
        }
        else {
            salvoLocations.setTurn(gamePlayerRepository.getById(gamePlayerId).getSalvo().size() + 1);
        }
        if (playerId.getId() < player2Id.getId()){
            if (player1 == player2){
                salvoRepository.save(salvoLocations);
                return new ResponseEntity<>(makeMap("OK", "Salvos creados"), HttpStatus.CREATED);
            }
            return new ResponseEntity<>(makeMap("error", "Espera a tu turno"), HttpStatus.FORBIDDEN);
        }
        if (player2 > player1){
            salvoRepository.save(salvoLocations);
            return new ResponseEntity<>(makeMap("OK", "Salvos creados"), HttpStatus.CREATED);
            }
        return new ResponseEntity<>(makeMap("error", "Espera a tu turno"), HttpStatus.FORBIDDEN);
    }

    @RequestMapping(value = "/games/players/{gamePlayerId}/salvoes", method = RequestMethod.GET)
    public ResponseEntity<Map> placeSalvos(@PathVariable Long gamePlayerId){
        if (gamePlayerRepository.findById(gamePlayerId).isPresent()){
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("salvoes", gamePlayerRepository.findById(gamePlayerId)
                    .get()
                    .getSalvo()
                    .stream()
                    .map(x -> x.makeSalvoDTO())
                    .collect(Collectors.toList()));
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        }
        return new ResponseEntity<>(makeMap("error", "No exist"), HttpStatus.FORBIDDEN);
    }

    private GamePlayer getOpponent(GamePlayer gamePlayer) {
        Long gamePlayerId = gamePlayer.getId();
        Set<GamePlayer> gamePlayers = gamePlayer.getGame().getGamePlayer();
        GamePlayer opponent = gamePlayers
                .stream()
                .filter(gp -> gp.getId() != gamePlayerId)
                .findFirst().orElse(null);
        return opponent;
    }

    public void setScores(GamePlayer gamePlayer){
        Game game = gamePlayer.getGame();
        Player player = gamePlayer.getPlayer();
        Player opponent = getOpponent(gamePlayer).getPlayer();
        if (getOpponent(gamePlayer).getGameState().equals("WON")){
            Score score = new Score(1, LocalDateTime.now(), player, game);
            Score scoreOpp = new Score(0, LocalDateTime.now(), opponent, game);
            scoreRepository.save(score);
            scoreRepository.save(scoreOpp);
        }
        if (getOpponent(gamePlayer).getGameState().equals("LOST")){
            Score score = new Score(0, LocalDateTime.now(), player, game);
            Score scoreOpp = new Score(1, LocalDateTime.now(), opponent, game);
            scoreRepository.save(score);
            scoreRepository.save(scoreOpp);
        }
        if (getOpponent(gamePlayer).getGameState().equals("TIE")){
            Score score = new Score(0.5, LocalDateTime.now(), player, game);
            Score scoreOpp = new Score(0.5, LocalDateTime.now(), opponent, game);
            scoreRepository.save(score);
            scoreRepository.save(scoreOpp);
        }
    }
}






