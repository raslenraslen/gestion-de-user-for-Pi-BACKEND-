package com.gamax.userservice.enums;
public enum BanReason {
    SPAM("Comportement toxique ou langage offensant"),
    HARASSMENT("Usurpation d'identité ou fausse information"),
    CHEATING(" Usurpation d'identité ou fausse information"),
    EXPLOIT("Exploitation de bugs ou failles du jeu");

    BanReason(String description) {
    }
    // ... constructeur/getter
}