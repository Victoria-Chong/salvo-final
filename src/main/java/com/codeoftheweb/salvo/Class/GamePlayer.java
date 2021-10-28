package com.codeoftheweb.salvo.Class;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    private LocalDateTime joinDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="player")
    private Player player;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="game")
    private Game game;

    @OneToMany(mappedBy="gamePlayer", fetch=FetchType.EAGER)
    Set<Ship> ships;

    @OneToMany(mappedBy="gamePlayer", fetch=FetchType.EAGER)
    @OrderBy
    Set<Salvo> salvo;

    @ElementCollection
    @Column(name = "self")
    private List<String> self = new ArrayList<>();

    @ElementCollection
    @Column(name = "opponent")
    private List<String> opponent = new ArrayList<>();

    public GamePlayer(){}

    public GamePlayer(Player player, Game game, LocalDateTime joinDate) {
        this.joinDate = joinDate;
        this.player = player;
        this.game = game;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Set<Ship> getShips() {
        return ships;
    }

    public void setShips(Set<Ship> ships) {
        this.ships = ships;
    }

    public Set<Salvo> getSalvo() {
        return salvo;
    }

    public void setSalvo(Set<Salvo> salvo) {
        this.salvo = salvo;
    }

    public List<String> getSelf() {
        return self;
    }

    public void setSelf(List<String> self) {
        this.self = self;
    }

    public void setOpponent(List<String> opponent) {
        this.opponent = opponent;
    }

    public Optional<Score> getScore(){
        return this.getPlayer().getScore(this.game);
    }

    public GamePlayer getOpponent() {
        GamePlayer opponent = this.getGame()
                .getGamePlayer()
                .stream()
                .filter(x -> x.getId() != this.getId())
                .findFirst().orElse(null);
        return opponent;
    }

    public Map<String, Object> makeGamePlayerDTO(){
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", this.getId());
        dto.put("player", this.getPlayer().makePlayerDTO());
        return dto;
    }

    public Map<String, Object> makeGameViewDTO(){
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", this.getGame().getId());
        dto.put("created", this.getGame().getCreationDate());
        dto.put("gameState", this.getGameState());
        dto.put("gamePlayers", this.getGame().getGamePlayer()
                .stream()
                .map(x -> x.makeGamePlayerDTO())
                .collect(Collectors.toList()));
        dto.put("ships", this.getShips()
                .stream()
                .map(x -> x.makeShipDTO())
                .collect(Collectors.toList()));
        dto.put("salvoes", this.getGame().getGamePlayer()
                .stream()
                .flatMap(x -> x.getSalvo()
                        .stream()
                        .map(y -> y.makeSalvoDTO()))
                .collect(Collectors.toList()));
        dto.put("hits", this.makeHitsDTO());
        return dto;
    }

    public String getGameState(){
        String waitOpponent = "WAITINGFOROPP";
        String wait = "WAIT";
        String play = "PLAY";
        String placeShips = "PLACESHIPS";
        String won = "WON";
        String lost = "LOST";
        String tie = "TIE";
        String undefined = "UNDEFINED";

        if (this.getOpponent() == null){
            return waitOpponent;
        }
        if (this.getShips().size() == 0){
            return placeShips;
        }
        if ((this.getSalvo().size() > 2 && getOpponent().getSalvo().size() > 2)
                && (this.getSalvo().size() == getOpponent().getSalvo().size())){

            String won1 = "";
            String won2 = "";

            if (shipsSunk(this,getOpponent())) {
                won1 = "1";
            }
            if (shipsSunk(getOpponent(),this)) {
                won2 = "1";
            }
            if (won1.equals("1") && won2.equals("1")) {
                return tie;
            }
            if (won1.equals("1")) {
                return lost;
            }
            if (won2.equals("1")) {
                return won;
            }
        }
        if (this.getOpponent() != null){
            int self1 = this.getSalvo().size();
            int opponent1 = this.getOpponent().getSalvo().size();
            if (this.getId() < this.getOpponent().getId()){
                if (self1 == opponent1){
                    return play;
                }
            }
            if (opponent1 > self1){
                return play;
            }
        }
        return wait;
    }

    private boolean shipsSunk(GamePlayer myShips, GamePlayer mySalvos){
        if (!myShips.getShips().isEmpty() && !mySalvos.getSalvo().isEmpty()){
            return mySalvos.getSalvo().stream()
                    .flatMap(x -> x.getSalvoLocations().stream())
                    .collect(Collectors.toList()).containsAll(myShips.getShips()
                            .stream()
                            .flatMap(y -> y.getShipLocations().stream())
                            .collect(Collectors.toList()));
        }
        return false;
    }

    public Map<String, Object> makeHitsDTO(){
        Map<String, Object> dto = new LinkedHashMap<>();
        if (this.getShips().size() > 0){
            dto.put("self", this.getHits());
            if (this.getOpponent() == null){
                dto.put("opponent", opponent);
            }
            else {
                dto.put("opponent", this.getOpponent().getHits());
            }
        }
        else {
            dto.put("self", self);
            dto.put("opponent", opponent);
        }
        return dto;
    }

    public List<Map<String, Object>> getHits(){
        List<Map<String, Object>> dto = new ArrayList<>();

        int carrierTotal = 0;
        int battleshipTotal = 0;
        int submarineTotal = 0;
        int destroyerTotal = 0;
        int patrolboatTotal = 0;


        if (this.getGameState() == "PLAY" || this.getGameState() == "WAIT"){
            List<String> carrierLocations = new ArrayList<>();
            List<String> battleshipLocations = new ArrayList<>();
            List<String> submarineLocations = new ArrayList<>();
            List<String> destroyerLocations = new ArrayList<>();
            List<String> patrolboatLocations = new ArrayList<>();

            carrierLocations = this.getShips()
                    .stream()
                    .filter(x -> x.getType().equals("carrier"))
                    .findFirst()
                    .get()
                    .getShipLocations();
            battleshipLocations = this.getShips()
                    .stream()
                    .filter(x -> x.getType().equals("battleship"))
                    .findFirst()
                    .get()
                    .getShipLocations();
            submarineLocations = this.getShips()
                    .stream()
                    .filter(x -> x.getType().equals("submarine"))
                    .findFirst()
                    .get()
                    .getShipLocations();
            destroyerLocations = this.getShips()
                    .stream()
                    .filter(x -> x.getType().equals("destroyer"))
                    .findFirst()
                    .get()
                    .getShipLocations();
            patrolboatLocations = this.getShips()
                    .stream()
                    .filter(x -> x.getType().equals("patrolboat"))
                    .findFirst()
                    .get()
                    .getShipLocations();

            for (Salvo salvoOpp : getOpponent().getSalvo()){
                int carrierHits = 0;
                int battleshipHits = 0;
                int submarineHits = 0;
                int destroyerHits = 0;
                int patrolboatHits = 0;

                Map<String, Object> hitsDTO = new LinkedHashMap<>();
                Map<String, Object> damages = new LinkedHashMap<>();
                List<String> salvoLocations = new ArrayList<>();

                salvoLocations.addAll(salvoOpp.getSalvoLocations());
                List<String> hitLocation = new ArrayList<>();

                int missedShots = salvoOpp.getSalvoLocations().size();

                for (String salvoShot : salvoLocations){
                    if (carrierLocations.contains(salvoShot)){
                        carrierHits++;
                        carrierTotal++;
                        hitLocation.add(salvoShot);
                        missedShots--;
                    }
                    if (battleshipLocations.contains(salvoShot)){
                        battleshipHits++;
                        battleshipTotal++;
                        hitLocation.add(salvoShot);
                        missedShots--;
                    }
                    if (submarineLocations.contains(salvoShot)){
                        submarineHits++;
                        submarineTotal++;
                        hitLocation.add(salvoShot);
                        missedShots--;
                    }
                    if (destroyerLocations.contains(salvoShot)){
                        destroyerHits++;
                        destroyerTotal++;
                        hitLocation.add(salvoShot);
                        missedShots--;
                    }
                    if (patrolboatLocations.contains(salvoShot)){
                        patrolboatHits++;
                        patrolboatTotal++;
                        hitLocation.add(salvoShot);
                        missedShots--;
                    }
                }
                hitsDTO.put("turn", salvoOpp.getTurn());
                hitsDTO.put("hitLocations", hitLocation);
                damages.put("carrierHits", carrierHits);
                damages.put("battleshipHits", battleshipHits);
                damages.put("submarineHits", submarineHits);
                damages.put("destroyerHits", destroyerHits);
                damages.put("patrolboatHits", patrolboatHits);
                damages.put("carrier", carrierTotal);
                damages.put("battleship", battleshipTotal);
                damages.put("submarine", submarineTotal);
                damages.put("destroyer", destroyerTotal);
                damages.put("patrolboat", patrolboatTotal);
                hitsDTO.put("damages", damages);
                hitsDTO.put("missed", missedShots);
                dto.add(hitsDTO);
            }
        }

        return dto;
    }
}




