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
 * Test the LDAP connection.
 *
 * @author jasonbruwer on 10/29/18.
 * @since 1.0
 */
public class LDAP {
	public static final String DOMAIN = "SAIBNET2\\";
	public static final String AD_SERVER = "ldap://saibnt922.saibnet2.saib.com:389";
	public static final String SEARCH_BASE = "dc=SAIBNET2,dc=saib,dc=com";

	/**
	 *
	 * @param ldapUsernameParam
	 * @param ldapPasswordParam
	 * @throws NamingException
	 */
	public static void authenticateLDAP(String ldapUsernameParam, String ldapPasswordParam)
	throws NamingException {
		if (ldapUsernameParam.indexOf("\\") < 0) {
			ldapUsernameParam = (DOMAIN+ldapUsernameParam);
		}

		final String ldapUsername = ldapUsernameParam;//myLdapUsername
		final String ldapPassword = ldapPasswordParam;//myLdapPassword

		//final String ldapAccountToLookup = "myOtherLdapUsername";

		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		if (ldapUsername != null) {
			env.put(Context.SECURITY_PRINCIPAL, ldapUsername);
		}
		if (ldapPassword != null) {
			env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
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
				ctx, SEARCH_BASE, ldapUsername);

		System.out.println("Login valid. LDAP: "+((srLdapUser == null) ? "[No SearchResult]" : srLdapUser.getAttributes()));

		//2) get the SID of the users primary group
		//TODO String primaryGroupSID = ldap.getPrimaryGroupSID(srLdapUser);

		//3) get the users Primary Group
		//TODO String primaryGroupName = ldap.findGroupBySID(ctx, ldapSearchBase, primaryGroupSID);
	}

	/**
	 *
	 * @param ctx
	 * @param ldapSearchBase
	 * @param accountName
	 * @return
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
		if(results.hasMoreElements()) {
			searchResult = results.nextElement();

			//make sure there is not another item available, there should be only 1 match
			if(results.hasMoreElements()) {
				System.err.println("Matched multiple users for the accountName: " + accountName);
				return null;
			}
		}

		return searchResult;
	}

    /**
     * 
     * @param ctx
     * @param ldapSearchBase
     * @param sid
     * @return
     * @throws NamingException
     */
	public String findGroupBySID(DirContext ctx, String ldapSearchBase, String sid) throws NamingException {

		String searchFilter = "(&(objectClass=group)(objectSid=" + sid + "))";

		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		NamingEnumeration<SearchResult> results = ctx.search(ldapSearchBase, searchFilter, searchControls);

		if(results.hasMoreElements()) {
			SearchResult searchResult = (SearchResult) results.nextElement();

			//make sure there is not another item available, there should be only 1 match
			if(results.hasMoreElements()) {
				System.err.println("Matched multiple groups for the group with SID: " + sid);
				return null;
			} else {
				return (String)searchResult.getAttributes().get("sAMAccountName").get();
			}
		}
		return null;
	}

	public String getPrimaryGroupSID(SearchResult srLdapUser) throws NamingException {
		byte[] objectSID = (byte[])srLdapUser.getAttributes().get("objectSid").get();
		String strPrimaryGroupID = (String)srLdapUser.getAttributes().get("primaryGroupID").get();

		String strObjectSid = decodeSID(objectSID);

		return strObjectSid.substring(0, strObjectSid.lastIndexOf('-') + 1) + strPrimaryGroupID;
	}

	/**
	 * The binary data is in the form:
	 * byte[0] - revision level
	 * byte[1] - count of sub-authorities
	 * byte[2-7] - 48 bit authority (big-endian)
	 * and then count x 32 bit sub authorities (little-endian)
	 *
	 * The String value is: S-Revision-Authority-SubAuthority[n]...
	 *
	 * Based on code from here - http://forums.oracle.com/forums/thread.jspa?threadID=1155740&tstart=0
	 */
	public static String decodeSID(byte[] sid) {
		final StringBuilder strSid = new StringBuilder("S-");
		// get version
		final int revision = sid[0];
		strSid.append(Integer.toString(revision));

		//next byte is the count of sub-authorities
		final int countSubAuths = sid[1] & 0xFF;

		//get the authority
		long authority = 0;
		//String rid = "";
		for(int i = 2; i <= 7; i++) {
			authority |= ((long)sid[i]) << (8 * (5 - (i - 2)));
		}
		strSid.append("-");
		strSid.append(Long.toHexString(authority));

		//iterate all the sub-auths
		int offset = 8;
		int size = 4; //4 bytes for each sub auth
		for (int j = 0; j < countSubAuths; j++) {
			long subAuthority = 0;
			for (int k = 0; k < size; k++) {
				subAuthority |= (long)(sid[offset + k] & 0xFF) << (8 * k);
			}

			strSid.append("-");
			strSid.append(subAuthority);

			offset += size;
		}

		return strSid.toString();
	}

}
