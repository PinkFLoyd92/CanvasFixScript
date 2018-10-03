package interfaces;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileReader;

import java.io.FileWriter;
import java.math.BigInteger;

import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class StudentUpdater {

	Connection conn = null;

	public StudentUpdater(Connection conn) throws Exception {
		this.conn = conn;
	}
	
	public void updateStudents() {
		String sql = "select user_id,unique_id,sis_user_id from pseudonyms where unique_id in( select name from users where name=sortable_name) and sis_user_id like '2018%'";
		ResultSet rsGetPseudonyms;
		Statement stmtGetPseudonyms;
		
		try {
			stmtGetPseudonyms = conn.createStatement();
			rsGetPseudonyms = stmtGetPseudonyms.executeQuery(sql);
			
			while(rsGetPseudonyms.next()) {
				String unique_id = rsGetPseudonyms.getString("unique_id");
				String sis_user_id = rsGetPseudonyms.getString("sis_user_id");
				long user_id = rsGetPseudonyms.getLong("user_id");
				fixStudentName(user_id, unique_id, sis_user_id);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void fixStudentName(long user_id, String userEspol, String matricula) {
		System.out.println("Fixing User: " + user_id + 
				" " + userEspol + " " + matricula);	
		
		ResultSet rsGetMigUsuarios;
		Statement stmtGetMigUsuarios;
		
		String sqlMigUsuarios = "select * from mig_usuarios where username='"+userEspol + "' limit 1";
		
		try {
			stmtGetMigUsuarios = conn.createStatement();
			rsGetMigUsuarios = stmtGetMigUsuarios.executeQuery(sqlMigUsuarios);
			
			if(rsGetMigUsuarios.next()) {
				String nombre = rsGetMigUsuarios.getString("nombres");
				String apellidos = rsGetMigUsuarios.getString("apellidos");
				System.out.println("User: " + nombre + 
						" " + apellidos + " ");	
				
				String sqlUpdateUser = "update users set name=?,sortable_name=? where id=?";
				
				PreparedStatement pStmtUpdateUser = conn.prepareStatement(sqlUpdateUser);
				
				pStmtUpdateUser.setString(1, nombre + " " + apellidos);
				pStmtUpdateUser.setString(2, apellidos + "," + nombre);
				pStmtUpdateUser.setLong(3, user_id);
				pStmtUpdateUser.executeUpdate();
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
	