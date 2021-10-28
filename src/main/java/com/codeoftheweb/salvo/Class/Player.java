package com.codeoftheweb.salvo.Class;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    private String userName;
    private String password;

    @OneToMany(mappedBy="player", fetch=FetchType.EAGER)
    Set<GamePlayer> gamePlayer;

    @OneToMany(mappedBy="player", fetch=FetchType.EAGER)
    Set<Score> score;

    public Player(){}

    public Player(String userName) {
        this.userName = userName;
    }

    public Long getId() {
        return id;
    }

    public Map<String, Object> makePlayerDTO(){
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", this.getId());
        dto.put("email", this.getuserName());
        return dto;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Player(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getuserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<GamePlayer> getGamePlayer() {
        return gamePlayer;
    }

    public void setGamePlayer(Set<GamePlayer> gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    public Set<Score> getScore() {
        return score;
    }

    public void setScore(Set<Score> score) {
        this.score = score;
    }

    public Optional<Score> getScore(Game juego){
        return this.getScore()
                .stream()
                .filter(x -> x.getGame().getId()
                        .equals(juego.getId())).findFirst();
    }

    @JsonIgnore
    public List<Game> getGame() {
        return gamePlayer
                .stream()
                .map(sub -> sub.getGame())
                .collect(Collectors.toList());
    }
}
