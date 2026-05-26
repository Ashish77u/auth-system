package com.codelab.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be 3–20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}


/*

*** DTO (Data Transfer Object) hai jo registration ke liye client se data receive karta hai. Isme validation annotations hain jaise @NotBlank, @Size, aur @Email, jo ensure karte hain ki client se sahi format mein data aaye. Ye RegisterRequest class registration endpoint par use hoti hai jab user apna username, email, aur password submit karta hai. Validation fail hone par appropriate error messages return kiye jayenge.

*** DTOs important hai ya nhi?

-- kay bina DTOs ke bhi hum kaam kar sakte hain, yes or no?
    -- problem ye hai ki agar hum DTOs ka use nahi karenge, toh hum directly apne entity classes ko client ke saath interact karne ke liye expose kar denge. Ye security risk ho sakta hai, aur data integrity issues bhi create kar sakta hai. DTOs humein ek layer of abstraction provide karte hain jisse hum apne internal data models ko client se alag rakh sakte hain, aur validation bhi easily implement kar sakte hain.

-- DTOs important hai kyunki ye humein data validation, security, aur separation of concerns provide karte hain. Agar hum DTOs ka use nahi karenge, toh hum apne entities ko directly expose kar denge, jo ki best practice nahi hai. DTOs se hum apne API ko clean aur maintainable bana sakte hain.


*** flow of client se server tak data kaise jata hai registration ke case mein: ( first without DTOs, then with DTOs )

-- first without DTOs:
1. Client apna registration form fill karta hai name, email, aur password ke saath then submit karta hai.
2. Server par ek endpoint hota hai /api/auth/register jo directly User entity ko receive karta hai.
3. Server par validation hoti hai ki email valid hai ya nahi, password strong hai ya nahi, etc. Agar validation fail hoti hai toh error response return hota hai.
4. Agar validation pass hoti hai toh User entity ko database mein save kar diya jata hai.
5. Server par registration success response return hota hai.

-- then with DTOs:
1. Client apna registration form fill karta hai name, email, aur password ke saath then submit karta hai.
2. Server par ek endpoint hota hai /api/auth/register jo RegisterRequest DTO ko receive karta hai.
3. Server par DTO ke fields par validation annotations ke through validation hoti hai automatically. Agar validation fail hoti hai toh error response return hota hai with specific messages.
4. Agar validation pass hoti hai toh server par ek service method call hota hai jo RegisterRequest DTO ko User entity mein convert karta hai, aur phir User entity ko database mein save kar diya jata hai.
5. Server par registration success response return hota hai.    

*** so Doon me different kya hai? 
    -- without DTOs mein hum directly User entity ko receive kar rahe hain, jo ki security risk ho sakta hai aur data integrity issues create kar sakta hai. With DTOs mein hum ek separate class (RegisterRequest) use kar rahe hain jo client se data receive karta hai, aur usme validation annotations hain jo ensure karte hain ki data sahi format mein aaye. Isse hum apne internal data models ko client se alag rakh sakte hain, aur validation bhi easily implement kar sakte hain. DTOs se hum apne API ko clean aur maintainable bana sakte hain.


*** DTOs me Request and Response ka role kya hai?
    -- Request DTOs wo classes hoti hain jo client se data receive karne ke liye use hoti hain, jaise ki RegisterRequest. Inme validation annotations hote hain jo ensure karte hain ki client se sahi format mein data aaye. 
    
    --Response DTOs wo classes hoti hain jo server se client ko data bhejne ke liye use hoti hain, jaise ki AuthResponse. Inme wo fields hote hain jo hum client ko return karna chahte hain, jaise ki JWT token, user info, etc. Request DTOs aur Response DTOs dono hi API design mein important role play karte hain kyunki ye humein data transfer ke liye clear contracts define karne mein madad karte hain.

    -- example ke liye, RegisterRequest ek Request DTO hai jo client se registration data receive karta hai, aur AuthResponse ek Response DTO hai jo server se client ko authentication result return karta hai. Dono hi DTOs humein apne API ko clean aur maintainable banane mein madad karte hain.

    



*/
