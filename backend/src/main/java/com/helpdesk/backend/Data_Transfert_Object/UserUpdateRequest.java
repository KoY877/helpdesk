package com.helpdesk.backend.Data_Transfert_Object;

public record UserUpdateRequest(
    String name,
    String email,
    String password
) {}
