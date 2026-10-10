package com.umedical.umedical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateSpecialtyRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String name;

    @Size(max = 255, message = "La descripcion no puede superar 255 caracteres")
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

// package com.umedical.umedical.dto;

// import jakarta.validation.constraints.NotBlank;

// public class CreateSpecialtyRequest {

//     @NotBlank(message = "El nombre es obligatorio")
//     private String name;

//     private String description;

//     public String getName() { return name; }
//     public void setName(String name) { this.name = name; }
//     public String getDescription() { return description; }
//     public void setDescription(String description) { this.description = description; }
// }