/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.tutorial.helloworld;

import org.eclipse.jetty.server.session.SessionHandler;
import org.kurento.client.KurentoClient;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.bundles.webjars.WebJarBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Hello World (WebRTC in loobpack) main class.
 *
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 1.0.0
 */
public class HelloWorldApp extends Application<HelloWorldConfig> {

  public static void main(String[] args) throws Exception {
    new HelloWorldApp().run(args);
  }

  @Override
  public String getName() {
    return "hello-world";
  }

  @Override
  public void initialize(Bootstrap<HelloWorldConfig> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/static", "/", "index.html", "html"));
    bootstrap.addBundle(new AssetsBundle("/static/js", "/js", "index.js", "js"));
    bootstrap.addBundle(new AssetsBundle("/static/css", "/css", null, "css"));
    bootstrap.addBundle(new AssetsBundle("/static/img", "/img", null, "img"));
    bootstrap.addBundle(new WebJarBundle("org.webjars.bower"));

  }

  @Override
  public void run(HelloWorldConfig configuration, Environment environment) {
    environment.jersey().register(new HelloWorldResource(KurentoClient.create()));
    environment.servlets().setSessionHandler(new SessionHandler());
    environment.jersey().setUrlPattern("/api/*");
  }

}
