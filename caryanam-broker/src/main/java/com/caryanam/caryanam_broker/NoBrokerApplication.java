package com.caryanam.caryanam_broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NoBrokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(NoBrokerApplication.class, args);

		System.out.println("Caryanam Broker Application start Successfully");
		System.out.println("\n\n");
		System.err.println("PORT : localhost8080");

		System.err.println("  *****    *******  *******       *****   *******    *****    ******   *******" );
		System.err.println(" *     *   *      *    *         *           *      *     *   *     *     *   " );
		System.err.println("*       *  *      *    *         *           *     *       *  *     *     *   " );
		System.err.println("*       *  *******     *          *****      *     *       *  ******      *   " );
		System.err.println("*********  * 	       *               *     *     *********  *   *       *   " );
		System.err.println("*       *  *           *               *     *     *       *  *    *      *   " );
		System.err.println("*       *  *        *******       *****      *     *       *  *     *     *   " );

	}
}
