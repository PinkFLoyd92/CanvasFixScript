package interfaces;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import interfaces.LinksFixer.Fixer;

public class main {
	public static void main(String [] args) {
		Connection conn = null;
		
		String srv = "localhost";
		String db = "canvas_restore_20180925";
		String usr = "postgres";
		String pwd = "123";
		
		String strConn= "jdbc:postgresql://"+ srv +":5432/"+ db + "?" + "user=" + usr + "&password=" + pwd;
		try {
			conn = DriverManager.getConnection(strConn);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		System.out.println("Starting...");

		Fixer fixer = new Fixer(conn);
		Attachments attachments = new Attachments(conn);
		try {
			attachments.copiarFolderAttachments(14323, 20190);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		fixer.fixEnrollmentNamesOfTerm(67);
		fixer.fixFilesInWikiPages(67);
		//fixer.fixQuestionsFromTerm(67);

		//fixer.fixEnrollmentNamesOfTerm(68);
		//fixer.fixQuestionsFromTerm(68);

		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
