/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2020] Koekiebox (Pty) Ltd
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of Koekiebox and its suppliers, if any. The intellectual and
 * technical concepts contained herein are proprietary to Koekiebox
 * and its suppliers and may be covered by South African and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material is strictly
 * forbidden unless prior written permission is obtained from Koekiebox Innovations.
 */

package com.fluidbpm.fluidwebkit.backing.bean.login.ldap;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;

/**
 * Class for using LDAP authentication.
 *
 * @author jasonbruwer on 10/29/18.
 * @since 1.0
 */
public class LDAP {
	public static String DOMAIN = null;
	public static String AD_SERVER = null;
	public static String SEARCH_BASE = null;

	/**
	 * Authenticate using the LDAP protocol.
	 *
	 * @param ldapUsernameParam Username (Domain is optional).
	 * @param ldapPasswordParam The user password.
	 * @throws NamingException For JNDI lookup failures.
	 */
	public static void authenticateLDAP(String ldapUsernameParam, String ldapPasswordParam)
	throws NamingException {
		if (ldapUsernameParam.indexOf("\\") < 0) {
			ldapUsernameParam = (DOMAIN+ldapUsernameParam);
		}

		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		if (ldapUsernameParam != null) {
			env.put(Context.SECURITY_PRINCIPAL, ldapUsernameParam);
		}
		if (ldapPasswordParam != null) {
			env.put(Context.SECURITY_CREDENTIALS, ldapPasswordParam);
		}
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, AD_SERVER);

		//ensures that objectSID attribute values
		//will be returned as a byte[] instead of a String
		env.put("java.naming.ldap.attributes.binary", "objectSID");

		// the following is helpful in debugging errors
		//env.put("com.sun.jndi.ldap.trace.ber", System.err);

		LdapContext ctx = new InitialLdapContext(env, null);

		LDAP ldap = new LDAP();

		//1) lookup the ldap account
		SearchResult srLdapUser = ldap.findAccountByAccountName(
				ctx, SEARCH_BASE, ldapUsernameParam);

		System.out.println("Login valid. LDAP: "+((srLdapUser == null) ? "[No SearchResult]" : srLdapUser.getAttributes()));
	}

	/**
	 * Lookup the account by its mane.
	 *
	 * @param ctx The JNDI DirContext.
	 * @param ldapSearchBase The LDAP Search base.
	 * @param accountName The name of the account to lookup.
	 * @return {@code SearchResult} For JNDI lookup failures.
	 * 
	 * @throws NamingException
	 */
	public SearchResult findAccountByAccountName(
			DirContext ctx,
			String ldapSearchBase,
			String accountName
    ) throws NamingException {
		String searchFilter = "(&(objectClass=user)(sAMAccountName=" + accountName + "))";

		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		//searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);

		NamingEnumeration<SearchResult> results =
				ctx.search(ldapSearchBase, searchFilter, searchControls);

		SearchResult searchResult = null;
		if (results.hasMoreElements()) {
			searchResult = results.nextElement();

			//make sure there is not another item available, there should be only 1 match
			if (results.hasMoreElements()) {
				System.err.println("Matched multiple users for the accountName: " + accountName);
				return null;
			}
		}

		return searchResult;
	}
}
