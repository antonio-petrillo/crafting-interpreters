package com.craftinginterpreters.lox;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import static com.craftinginterpreters.lox.TokenType.*;

import org.junit.jupiter.api.Test;

public class EnvironmentTest {

    private static void assertEnvGetMatchValue(Environment env, Token token, LoxValue expected) {
        try {
            LoxValue actual = env.get(token);
            assertEquals(expected, actual, String.format("Expected value to be <%s>, got <%s>.", expected, actual));
        } catch(EnvironmentException ee) {
            assertTrue(false, String.format("Expected Env to contain %s, but it doesn't.", token.lexeme()));
        }
    }

    private static void assertEnvThrows(Environment env, Token token) {
        try {
            LoxValue actual = env.get(token);
            assertTrue(false, String.format("Expected Env to throw exception on getting <%s>.", token.lexeme()));
        } catch(EnvironmentException ee) {
        }
    }

    @Test
    public void shouldDefineCorrectly() {
        Environment env = new Environment();
        Token a = new Token(IDENTIFIER, "a", Optional.empty(), 1);

        LoxValue str = new LoxStr("asdfaf");
        env.define(a.lexeme(), str);
        assertEnvGetMatchValue(env, a, str);
    }

    @Test
    public void shouldAssignCorrectly() {
        Environment env = new Environment();
        Token a = new Token(IDENTIFIER, "a", Optional.empty(), 1);

        LoxValue str1 = new LoxStr("asdfaf");
        LoxValue str2 = new LoxStr("nfqsns");
        env.define(a.lexeme(), str1);
        assertEnvGetMatchValue(env, a, str1);
        try {
            env.assign(a, str2);
        } catch (EnvironmentException ee) {
            assertTrue(false, String.format("Should assign because '%s' is already defined.", a));
        }
    }

    @Test
    public void shouldAssignInOuterEnv() {
        Environment outer = new Environment();
        Token a = new Token(IDENTIFIER, "a", Optional.empty(), 1);

        LoxValue str1 = new LoxStr("asdfaf");
        LoxValue str2 = new LoxStr("nfqsns");
        outer.define(a.lexeme(), str1);
        assertEnvGetMatchValue(outer, a, str1);
        Environment env = new Environment(outer);
        try {
            env.assign(a, str2);
            assertEnvGetMatchValue(env, a, str2);
        } catch (EnvironmentException ee) {
            assertTrue(false, String.format("Should assign because '%s' is already defined in outer scope.", a));
        }
    }

    @Test
    public void shouldThrowIfAssignToUnknownVar() {
        Environment env = new Environment();
        Token a = new Token(IDENTIFIER, "a", Optional.empty(), 1);

        LoxValue str = new LoxStr("asdfaf");
        try {
            env.assign(a, str);
            assertTrue(false, String.format("Should fail because '%s' is not defined.", a));
        } catch (EnvironmentException ee) {
        }
    }

    @Test
    public void shouldThrowIfAssignToUnknownVarWithOuterEnv() {
        Environment outer = new Environment();
        Environment env = new Environment(outer);
        Token a = new Token(IDENTIFIER, "a", Optional.empty(), 1);

        LoxValue str = new LoxStr("asdfaf");
        try {
            env.assign(a, str);
            assertTrue(false, String.format("Should fail because '%s' is not defined in None of the scopes.", a));
        } catch (EnvironmentException ee) {
        }
    }

    @Test
    public void shouldFindInOuterEnv() {
        Environment env = new Environment();
        Token a = new Token(IDENTIFIER, "a", Optional.empty(), 1);

        LoxValue str = new LoxStr("asdfaf");
        env.define(a.lexeme(), str);
        Environment inner = new Environment(env);
        assertEnvGetMatchValue(inner, a, str);
    }

    @Test
    public void shouldThrowIfTokenIsNotPresent() {
        Environment env = new Environment();
        Token a = new Token(IDENTIFIER, "a", Optional.empty(), 1);

        assertEnvThrows(env, a);
    }

    @Test
    public void shouldThrowIfInstantiateWithANullOuterEnvironment() {
        assertThrows(IllegalArgumentException.class, () -> new Environment(null), "Should throw exception if instantiate with 'null' outer env.");
    }

    @Test
    public void shouldThrowIfTokenIsNotPresentEvenInOuterEnv() {
        Environment outer = new Environment();
        Environment env = new Environment(outer);
        Token a = new Token(IDENTIFIER, "a", Optional.empty(), 1);

        assertEnvThrows(env, a);
    }
}
