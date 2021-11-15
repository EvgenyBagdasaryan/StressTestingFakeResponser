package ru.diasoft.integration.vtb.service.stub.rest;

import com.sun.jersey.core.util.ReaderWriter;
import com.sun.jersey.spi.container.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.utils.ParamsUtil;
import ru.diasoft.integration.vtb.utils.Utl;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class RestLoggingFilter implements ResourceFilter, ContainerRequestFilter, ContainerResponseFilter {

    private static Logger logger = Logger.getLogger(RestLoggingFilter.class);

    @Context
    private Providers providers;

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        try {
            String user = (request.getSecurityContext().getUserPrincipal() == null)
                    ? "unknown" : ParamsUtil.getString(request.getSecurityContext().getUserPrincipal());

            String url = ParamsUtil.getString(request.getAbsolutePath());
            String command = Utl.getCommandFromRestUrl(url);
            String body = setRequestEntityBody(request);
            String entity = (StringUtils.isNotBlank(body)) ? body : "empty";
            request.getRequestHeaders().add("command", command);

            String msg = "fake HTTP REQUEST: User: " + user +
                    ";\n Path: " + url +
                    ";\n Command: " + command +
                    ";\n Header: " + request.getRequestHeaders() +
                    ";\n Entity: " + entity + "\n";
            logger.debug(msg);
        } catch (Exception e) {
            logger.error("fake RestLoggingFilter.filter.request error: " + e.getMessage());
        }
        return request;
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        try {
            String user = (request.getSecurityContext().getUserPrincipal() == null)
                    ? "unknown" : ParamsUtil.getString(request.getSecurityContext().getUserPrincipal());

            String url = ParamsUtil.getString(request.getAbsolutePath());
            String command = Utl.getCommandFromRestUrl(url);
            String body = getResponseEntity(response);
            String entity = (StringUtils.isNotBlank(body)) ? body : "empty";
            response.getHttpHeaders().add("command", command);

            String msg = "HTTP RESPONSE:  User: " + user +
                    ";\n Path: " + url +
                    ";\n Command: " + command +
                    ";\n Header: " + response.getHttpHeaders() +
                    ";\n Entity: " + entity + "\n";
            logger.debug(msg);

        } catch (Exception e) {
            logger.error("fake RestLoggingFilter.filter.response error: " + e.getMessage());
        }
        return response;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return this;
    }

    private String setRequestEntityBody(ContainerRequest request) {
        try {
            StringBuilder b = new StringBuilder();
            byte[] requestEntity = getRequestEntity(request);
            if (requestEntity.length > 0) {
                b.append(new String(requestEntity));
            }
            request.setEntityInputStream(new ByteArrayInputStream(requestEntity));
            return b.toString();

        } catch (Exception e) {
            logger.error("fake RestLoggingFilter.setRequestEntityBody error: " + e.getMessage());
            return "";
        }
    }

    private byte[] getRequestEntity(ContainerRequest request) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = request.getEntityInputStream();
            ReaderWriter.writeTo(in, out);
            return out.toByteArray();

        } catch (Exception e) {
            logger.error("fake RestLoggingFilter.getRequestEntity error: " + e.getMessage());
            return "".getBytes();
        }
    }

    private String getResponseEntity(ContainerResponse response) {
        try {
            String message = "Outgoing message".concat(System.lineSeparator());
            if (response.getMediaType() != null) {
                message = message.concat("Content-Type: ")
                        .concat(response.getMediaType().toString())
                        .concat(System.lineSeparator());
            }

            String contentType = (response.getHttpHeaders() != null)
                    ? String.valueOf(response.getHttpHeaders().get("Content-Type"))
                    : "";

            String body = getBody(response);

            if (contentType != null && contentType.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                message = message
                        .concat("BODY: ")
                        .concat((StringUtils.isNotBlank(body)) ? "fake content file in Base64" : "empty")
                        .concat(System.lineSeparator());
            } else {
                message = message
                        .concat("BODY: ")
                        .concat(body)
                        .concat(System.lineSeparator());
            }

            return message;

        } catch (Exception e) {
            logger.error("fake RestLoggingFilter.getResponseEntity error: " + e.getMessage());
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private String getBody(ContainerResponse response) {
        try {
            String message = "";
            if (response.getEntity() != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Class<?> entityClass = response.getEntity().getClass();
                Type entityType = response.getEntityType();
                Annotation[] entityAnnotations = response.getAnnotations();
                MediaType mediaType = response.getMediaType();

                MessageBodyWriter<Object> bodyWriter = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(entityClass,
                        entityType,
                        entityAnnotations,
                        mediaType);
                bodyWriter.writeTo(response.getEntity(),
                        entityClass,
                        entityType,
                        entityAnnotations,
                        mediaType,
                        response.getHttpHeaders(),
                        baos);
                message = message.concat(new String(baos.toByteArray()));
            }
            return message;

        } catch (Exception e) {
            logger.error("fake RestLoggingFilter.getBody error: " + e.getMessage());
            return "";
        }
    }
}
