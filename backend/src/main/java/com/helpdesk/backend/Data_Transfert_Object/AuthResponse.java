package com.helpdesk.backend.Data_Transfert_Object;


public record AuthResponse(
    String token,
    String role, 
    String userId
) {} 
