

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import models.sidweb.Folder;

public class Attachments {

	private Connection conn = null;
	private Statement stmt1 = null;	//para consultas largas		(EJM iteracion de cursos)
	private Statement stmt2 = null; //para consultas inmediatas (EJM validar que existe una columna)
	private PreparedStatement psf = null; //para insertar datos en la base
	private ResultSet rs1 = null;	
	private ResultSet rs2 = null;
	private HashMap<Long, Long> folder_ids = null;
	//private HashMap<Long, Long> attachment_ids = null;
	
	public Attachments(Connection conn) {
		this.conn = conn;
		this.folder_ids = new HashMap<Long, Long>();
	}

	public static String getValues(Map folder_ids) {
		StringBuilder strBuilder = new StringBuilder();
	    Iterator it = folder_ids.entrySet().iterator();

	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        strBuilder.append( "'"+pair.getValue()+"'," ); 
	    }
	    String filter = strBuilder.toString();
	    if(filter.length() > 0) {
	    	filter = filter.substring(0, filter.length()-1); 
	    }
	    return filter;
	}
	
	public HashMap<Long, Long> copiarFolders(long past_course_id, long new_course_id) {
		HashMap<Long, Long> folder_ids = new HashMap<Long, Long>();
		String sql=null;

		try {
			stmt1 = conn.createStatement();
			stmt2 = conn.createStatement();

			/* Obtenemos todos las carpetas del curso anterior. */
			ResultSet getFoldersQuery = 
					stmt1.executeQuery("SELECT * "
							+ "FROM folders "
							+ "WHERE context_type='Course' "
							+ "AND context_id= "+past_course_id+" "
							+ "AND workflow_state<>'deleted' "
							+ "AND (id>parent_folder_id  OR parent_folder_id IS NULL) "
							+ "ORDER BY id");
			
		while(getFoldersQuery.next()) {
			// En la primera iteracion tenemos al parent_root_folder
			Folder originFolder = new Folder(
					getFoldersQuery.getLong(1), 
					getFoldersQuery.getString(2), 
					getFoldersQuery.getString(3),
					getFoldersQuery.getLong(4),
					getFoldersQuery.getString(5),
					getFoldersQuery.getLong(6),
					getFoldersQuery.getString(7),
					getFoldersQuery.getInt(17),
					getFoldersQuery.getLong(19)
					);

			try {
				
				sql = "insert into folders (name, full_name,"
						+ "context_id, context_type, parent_folder_id,"
						+ "workflow_state,created_at, updated_at,position, migration_id) values ("
						+ "?, ?, ?,  ?, ?, ?, NOW(), NOW(), ?, ?)";
				
				psf = conn.prepareStatement(sql);
				
				psf.setString(1, originFolder.name); //name
				psf.setString(2, originFolder.full_name);	//full_name
				psf.setLong(3, new_course_id);		//context_id
				psf.setString(4, originFolder.context_type);	//context_type
				if(originFolder.parent_folder_id == 0L || folder_ids.get(originFolder.parent_folder_id) == null) psf.setNull(5, java.sql.Types.BIGINT);
				else psf.setLong(5, folder_ids.get(originFolder.parent_folder_id));	
				psf.setString(6, originFolder.workflow_state);	//workflow_state
				if((Integer)originFolder.position == null) psf.setNull(7, java.sql.Types.INTEGER);
				else psf.setInt(7, originFolder.position);
				psf.setLong(8, originFolder.id); // migration_id
				
				psf.executeUpdate();
				
				
			} catch(Exception e) { 
				System.out.println("RERORR INDEX_FOLDERS_ON_CONTEXT_ID...");
				
				// OBTENEMOS EL ROOT_FOLDER_ID DEL NUEVO CURSO
				rs2=stmt2.executeQuery("SELECT * FROM folders "
						+ "WHERE workflow_state<>'deleted' "
						+ "AND context_type='Course' AND context_id="+new_course_id+" "
						+ "AND parent_folder_id is NULL "
						+ "" );
				
				if(rs2.next()) {
					
					psf = conn.prepareStatement(sql);
				
					psf.setString(1, originFolder.name); //name
					psf.setString(2, originFolder.full_name);	//full_name
					psf.setLong(3, new_course_id);		//context_id
					psf.setString(4, originFolder.context_type);	//context_type
				    psf.setLong(5, rs2.getLong(1));	
				    psf.setString(6, originFolder.workflow_state);	//workflow_state
				    if((Integer)originFolder.position == null) psf.setNull(7, java.sql.Types.INTEGER);
				    else psf.setInt(7, originFolder.position);
				    psf.setLong(8, originFolder.id); // migration_id
				
				    psf.executeUpdate();
				}
			}
				
				rs2=stmt2.executeQuery("SELECT * FROM folders "
						+ "WHERE workflow_state<>'deleted' "
						+ "AND context_type='Course' AND context_id="+new_course_id+" "
						+ "AND full_name = '"+ originFolder.full_name.replaceAll("'", "''")+"'" );
				
				/* Ahora llenamos nuestro diccionario */
				if(rs2.next()) {
					/* 19 - MIGRATION_ID -- 1 - ID*/
					folder_ids.put(originFolder.id, rs2.getLong(1));
				}
				
		}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return folder_ids;
	}
	
	public void copiarFolderAttachments(long past_course_id, long new_course_id) throws SQLException {
		this.folder_ids = copiarFolders(past_course_id, new_course_id);

		String sql=null;
		stmt1 = conn.createStatement();
		stmt2 = conn.createStatement();
		
		rs1=stmt1.executeQuery("SELECT * "
				+ "FROM folders f LEFT JOIN attachments a ON f.id=a.folder_id "
				+ "WHERE f.context_type='Course' "
				+ "AND f.context_id= "+past_course_id+" "
				+ "AND f.workflow_state<>'deleted' AND (f.id>f.parent_folder_id  OR f.parent_folder_id IS NULL) "
				+ "AND ("
				+ "(a.workflow_state='processed' AND a.file_state='available' "
				+ "AND (a.id > a.root_attachment_id OR a.root_attachment_id IS NULL)) OR a.id IS NULL) "
				+ "ORDER BY f.id,a.id");

		while(rs1.next()) {

			//copia los attachments
			rs1.getLong(20);	//id del attachment
			String display_name = rs1.getString(28);
			if(display_name != null) display_name = display_name.replace("'", "''");
			if(!rs1.wasNull()) {//verifica que ese folder tenga algun archivo
				rs2=stmt2.executeQuery("SELECT * FROM attachments "
						+ "WHERE display_name= '"+display_name+"' "
						+ "AND folder_id="+folder_ids.get(rs1.getLong(1)));//verifica que no se repita el archivo
				if(!rs2.next()) {
					sql = "insert into attachments (context_id, context_type, size, "
							+ "folder_id, content_type, filename, uuid, display_name, "
							+ "created_at, updated_at, workflow_state, user_id, locked, "
							+ "file_state, position,root_attachment_id,migration_id,"
							+ "namespace,md5, encoding, modified_at) values ("
							+ "?, ?, ?, ?, ?, ?, uuid_generate_v1(), ?, NOW(), NOW(), ?, ?, ?, ?, ?,"
							+ "?, ?, ?, MD5(?), ?, NOW())";
							psf = conn.prepareStatement(sql);
							
							psf.setLong(1,new_course_id);		//context_id
							psf.setString(2,"Course");	//context_type
							rs1.getLong(23);		//size
							//System.out.println("sd "+rs1.toString());
							if(rs1.wasNull()) psf.setNull(3, java.sql.Types.BIGINT);
							else psf.setLong(3, rs1.getLong(23));
							rs1.getLong(24);			//folder_id
							if (rs1.wasNull())psf.setNull(4, java.sql.Types.BIGINT);
							else {
								long folder_id = 0L;
								try {
									folder_id = folder_ids.get(rs1.getLong(24));
								}catch(Exception e ) {
									e.printStackTrace();
								}
								if((Long)folder_id==null || folder_id == 0L){
									psf.setNull(4, java.sql.Types.BIGINT);
								} else {
									psf.setLong(4, folder_id);
								}
							}
							psf.setString(5,rs1.getString(25));	//content_type
							psf.setString(6,rs1.getString(26));	//filename
							psf.setString(7,rs1.getString(28)); //display_name
							psf.setString(8,"processed");		//workflow_state
							psf.setLong(9, 1);	//user_id
							psf.setBoolean(10, false);
							psf.setString(11,"available");		//file_state
							rs1.getLong(36);					//position
							if(rs1.wasNull()) psf.setNull(12, java.sql.Types.INTEGER);
							else psf.setLong(12, rs1.getLong(36));
							rs1.getLong(42);					//root_attachment_id
							
							if(rs1.wasNull()) psf.setLong(13, rs1.getLong(20));//id_attachment_viejo (root_attachment_id)
							 Long root_attachment_id = rs1.getLong(42);
							
							if(root_attachment_id==null || root_attachment_id==0) {
								//psf.setNull(4, java.sql.Types.BIGINT);
							} else root_attachment_id = rs1.getLong(42);

							if(root_attachment_id==null || root_attachment_id==0){
								psf.setLong(13, rs1.getLong(20));	//root_attachment_id
							}else{
								psf.setLong(13, root_attachment_id);	//root_attachment_id
							}
							//psf.setLong(13, rs1.getLong(20));	//root_attachment_id
							psf.setString(14, rs1.getString(20)); //migration_id -> id viejo
							psf.setString(15,"account_1");		//namespace
							psf.setString(16, rs1.getString(26));//md5
							psf.setString(17,rs1.getString(48));	//encoding
					psf.executeUpdate();
				}
			}
		}
	}	
}
