

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

	public StudentUpdater(String strConn) throws Exception {
		this.conn = DriverManager.getConnection(strConn);
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
				"\n" + userEspol + "\n" + matricula);	
	}
}
	