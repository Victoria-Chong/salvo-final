package com.codeoftheweb.salvo.Class;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    private LocalDateTime creationDate;

    @OneToMany(mappedBy="game", fetch=FetchType.EAGER)
    Set<GamePlayer> gamePlayer;

    @OneToMany(mappedBy="game", fetch=FetchType.EAGER)
    Set<Score> score;

    private Game(){}

    public Game(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Map<String, Object> makeGameDTO(){
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", this.getId());
        dto.put("created", this.getCreationDate());
        dto.put("gamePlayers", this.getGamePlayer()
                .stream()
                .map(x -> x.makeGamePlayerDTO())
                .collect(Collectors.toList()));
        dto.put("scores", this.getGamePlayer()
                .stream()
                .map(gamePlayer1 -> {
                    if (gamePlayer1.getScore().isPresent()){
                        return gamePlayer1.getScore().get().makeScoreDTO();
                    }
                    else {
                        return "";
                    }
                }));
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
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

    @JsonIgnore
    public List<Player> getPlayer() {
        return gamePlayer.stream().map(sub -> sub.getPlayer()).collect(Collectors.toList());
    }
}
