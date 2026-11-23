package com.doulos.wallet.controller;

import com.doulos.wallet.config.SecurityConfig;
import com.doulos.wallet.dto.usuario.CrearUsuarioDTO;
import com.doulos.wallet.dto.usuario.UsuarioResponseDTO;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.service.JwtService;
import com.doulos.wallet.service.UserDetailsServiceImpl;
import com.doulos.wallet.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private Usuario usuario;
    private Authentication authUsuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).nombre("Santiago").email("santiago@wallet.com").build();
        authUsuario = new UsernamePasswordAuthenticationToken(usuario, null, List.of());
    }

    @Nested
    @DisplayName("Endpoints Públicos (Registro y Login)")
    class EndpointsPublicosTests {

        @Test
        void registrarUsuario_SinToken_Retorna201YUsuario() throws Exception {
            Map<String, String> payload = Map.of(
                    "nombre", "Santiago",
                    "email", "santiago@wallet.com",
                    "password", "ClaveSegura123"
            );
            UsuarioResponseDTO respuesta = new UsuarioResponseDTO(1L, "Santiago", "santiago@wallet.com");

            when(usuarioService.crearUsuario(any(CrearUsuarioDTO.class))).thenReturn(respuesta);

            mockMvc.perform(post("/api/usuarios/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.email").value("santiago@wallet.com"));
        }

        @Test
        void iniciarSesion_CredencialesValidas_Retorna200YTokenJwt() throws Exception {
            Map<String, String> loginPayload = Map.of(
                    "email", "santiago@wallet.com",
                    "password", "ClaveSegura123"
            );
            Authentication authExitosa = new UsernamePasswordAuthenticationToken(usuario, null, List.of());

            when(authenticationManager.authenticate(any())).thenReturn(authExitosa);
            when(jwtService.generarToken(usuario)).thenReturn("jwt-token-simulado");

            mockMvc.perform(post("/api/usuarios/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginPayload)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("jwt-token-simulado")));
        }

        @Test
        void iniciarSesion_CredencialesInvalidas_Retorna4xx() throws Exception {
            Map<String, String> loginInvalido = Map.of(
                    "email", "santiago@wallet.com",
                    "password", "incorrecta"
            );

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Credenciales inválidas"));

            mockMvc.perform(post("/api/usuarios/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginInvalido)))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("Endpoints Protegidos (Consulta de Usuarios)")
    class EndpointsProtegidosTests {

        @Test
        void obtenerUsuarios_Autenticado_Retorna200YLista() throws Exception {
            UsuarioResponseDTO dto = new UsuarioResponseDTO(1L, "Santiago", "santiago@wallet.com");
            when(usuarioService.listarUsuarios()).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/usuarios")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nombre").value("Santiago"));

            // Frontera lista vacía
            when(usuarioService.listarUsuarios()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/usuarios")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void obtenerUsuarios_SinAutenticacion_Retorna403() throws Exception {
            mockMvc.perform(get("/api/usuarios"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void obtenerUsuarioPorId_Autenticado_Retorna200() throws Exception {
            UsuarioResponseDTO dto = new UsuarioResponseDTO(1L, "Santiago", "santiago@wallet.com");
            when(usuarioService.obtenerUsuarioId(1L)).thenReturn(dto);

            mockMvc.perform(get("/api/usuarios/{id}", 1L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }
    }
}