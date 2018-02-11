package com.jeseromero.controller;

import com.jeseromero.model.*;
import com.jeseromero.model.lightweight.JSONLightUser;
import com.jeseromero.persistence.DBSessionFactory;
import com.jeseromero.util.SLogger;
import org.hibernate.Session;

import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class UserController {

    private static HashMap<Token, User> tokens = new HashMap<>();

    private static final SLogger logger = new SLogger(UserController.class.getName());

    private static UserController instance = null;

    public static UserController instance() {
        if (instance == null) {
            instance = new UserController();
        }

        return instance;
    }

    public boolean exist(int userID) {
        Session session = DBSessionFactory.instance().openSession();

        try {
            return session.get(User.class, userID) != null;
        } finally {
            session.close();
        }
    }

    public JSONLightUser login(String username, String password) {
        logger.debug("Logging: " + username);

        Session session = DBSessionFactory.instance().openSession();

        try {

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<User> query = builder.createQuery(User.class);

            Root<User> root = query.from(User.class);

            query.select(root).where(builder.equal(root.get("username"), username));
            query.select(root).where(builder.equal(root.get("password"), password));

            User user = session.createQuery(query).getSingleResult();

            Token token = new Token();

            tokens.put(token, user);

            return new JSONLightUser(user, token);

        } catch (Exception e) {
            if (!(e instanceof NoResultException)) {
                logger.error(e);
            }

            logger.debug("User not found");

            return null;
        } finally {
            session.close();
        }
    }

    public Profile getProfile(Token token) {
        User user = tokens.get(token);

        Profile profile = null;

        if (user != null) {
            profile = new Profile();
            profile.setSearches(user.getSearches().stream().map(Search::getSearch).collect(Collectors.toSet()));
        }

        return profile;
    }

    public List<String> getMirrors(Token token) {

        User user = tokens.get(token);

        if (user != null) {
            user.refresh();

            return user.getMirrors().stream().filter(Mirror::isWorking).map(Mirror::getName).collect(Collectors.toList());
        }

        return null;
    }

    public boolean logout(Token token) {
        User user = tokens.get(token);

        if (user != null) {
            tokens.remove(token);
            return true;
        }

        return false;
    }

    public boolean isLogged(Token token) {
        return tokens.get(token) != null;
    }
}