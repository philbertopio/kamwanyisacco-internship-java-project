package com.kimwanyisacco.config;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContext;

@ApplicationScoped
public class CdiEntityManagerBridge {

    @Inject
    private ServletContext servletContext;

    @Produces
    @RequestScoped
    public EntityManager createEntityManager() {
        WebApplicationContext ctx =
                WebApplicationContextUtils.getWebApplicationContext(servletContext);
        EntityManagerFactory emf = ctx.getBean(EntityManagerFactory.class);
        return emf.createEntityManager();
    }

    public void closeEntityManager(@Disposes EntityManager em) {
        if (em.isOpen()) {
            em.close();
        }
    }
}