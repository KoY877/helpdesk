package com.helpdesk.backend.Data_Transfert_Object;


public record UserCreateRequest(
    String name,
    String email,
    String password
) {}
