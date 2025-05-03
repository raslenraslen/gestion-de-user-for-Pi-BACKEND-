package com.gamax.userservice.payload;
import lombok.Getter; // Si vous utilisez Lombok
import lombok.Setter; // Si vous utilisez Lombok


@Getter
@Setter
public class UsernameDescriptionRequest {
    private String description;



    public UsernameDescriptionRequest() {
    }


    public UsernameDescriptionRequest(String description) {
        this.description = description;
    }
}