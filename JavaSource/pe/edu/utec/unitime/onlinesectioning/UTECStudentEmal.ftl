<!DOCTYPE html>
<html>
 	<head>
 		<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>
 		<title><#if subject??>${subject}<#else>${msg.emailDeafultTitle()}</#if></title>
 	</head>
 	<body style="font-family: sans-serif, verdana, arial;">
 		<table style="padding: 5px; margin-top: 10px; min-width: 800px;" align="center" cellpadding="0" cellspacing="0">
 			<tr><td style='background: black; padding: 5px;'><img src="http://www.utec.edu.pe/_catalogs/masterpage/Themable/MasterUTEC/images/logo.png" alt="UTEC"></td></tr>
 			<tr><td style='background: rgb(0,173,216); text-align: center; padding: 5px;'>Confirmaci&oacute;n de matr&iacute;cula</td></tr>
 			<tr><td style='padding: 20px; 40px; 2px; text-align: justify;'>
 				<div style="text-align: left; padding-top: 20px; padding-bottom: 20px;">Estimado <#if name??>${name}<#else>${student.name}</#if></div>
 				<hr>
 				<div style="text-align: left; padding-top: 20px; padding-bottom: 20px;">Su matr&iacute;cula se efectu&oacute; de manera exitosa.</div>
 				<div style="text-align: left; padding-bottom: 20px;">Se ha matriculado en los siguientes cursos:</div>
 				<#if classes??>
 				<table width="100%" border="1" cellpadding="2" cellspacing="0">
 					<tr>
 						<th>C&oacute;digo de curso</th>
 						<th>Nombre del Curso</th>
 						<th>Secci&oacute;n matriculada</th>
 						<th>N&uacute;mero de cr&eacute;ditos</th>
 					</tr>
 					<#list classes as line>
 						<#if line.freeTime>
 						<#else><#if line.assigned && line.first>
 							<tr>
 								<td>${line.courseNumber}</td>
 								<td>${line.courseTitle}</td>
 								<td>${line.name}</td>
 								<td><#if line.credit??>${line.credit}</#if></td>
 							</tr>
 						</#if></#if>
 					</#list>
 					<tr>
 						<th colspan="3" style="text-align: right;">Total de cr&eacute;ditos matriculados:</th>
 						<td>${credit}</td>
 					</tr>
 				</table>
 				</#if>
 				<div style="text-align: left; padding-top: 20px;">
 					Si desea visualizar su horario puede dirigirse a la opci&oacute;n Asistente de Horarios del <b>Sistema de Matr&iacute;cula</b>.
 				</div>
 				<div style="text-align: left; padding-top: 40px; padding-bottom: 40px; font-weight: bold;">Servicios Educativos</div>
 				<hr>
 				<div style="text-align: left; padding-bottom: 20px;">
 					En el caso que tuviese alguna observaci&oacute;n a la informaci&oacute;n proporcionada, puede comunicarse a nuestra <b>Central de ayuda</b>.
 				</div>
 			</td></tr>
 		</table>
 	</body>
 </html>