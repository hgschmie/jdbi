/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * <p>
 * Classes here provide integration hooks for working with the Spring framework.
 * Jdbi instances may be obtained which will
 * behave correctly with Spring managed transactions.
 * </p>
 * <p>
 * Using the Spring facilities entails configuring Jdbi via the <code>JdbiFactoryBean</code> class,
 * and providing a DataSource with an associated transaction manager to that bean, such as:
 * </p>
 * <pre>
 * &lt;beans xmlns="http://www.springframework.org/schema/beans"
 * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 * xmlns:aop="http://www.springframework.org/schema/aop"
 * xmlns:tx="http://www.springframework.org/schema/tx"
 * xsi:schemaLocation="
 * http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.0.xsd
 * http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx-2.0.xsd
 * http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-2.0.xsd"&gt;
 * &lt;tx:annotation-driven transaction-manager="transactionManager"/&gt;
 * &lt;bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager"&gt;
 * &lt;property name="dataSource" ref="derby"/&gt;
 * &lt;/bean&gt;
 * &lt;bean id="derby" class="org.apache.derby.jdbc.EmbeddedDataSource" destroy-method="close"&gt;
 * &lt;property name="databaseName" value="testing"/&gt;
 * &lt;/bean&gt;
 * &lt;bean id="jdbi" class="org.jdbi.v3.spring5.JdbiFactoryBean"&gt;
 * &lt;property name="dataSource" ref="derby"/&gt;
 * &lt;/bean&gt;
 * &lt;bean id="service" class="org.jdbi.v3.spring5.DummyService"&gt;
 * &lt;constructor-arg ref="jdbi"/&gt;
 * &lt;/bean&gt;
 * &lt;/beans&gt;
 * </pre>
 *
 * The automatic detection of {@link org.jdbi.v3.spring5.JdbiRepository} can be enabled by using the
 * {@link org.jdbi.v3.spring5.EnableJdbiRepositories} annotation.
 *
 * @deprecated Use the {@link org.jdbi.v3.spring} module with Spring 6.x or newer.
 */
@Deprecated(forRemoval = true, since = "3.47.0")
package org.jdbi.v3.spring5;
