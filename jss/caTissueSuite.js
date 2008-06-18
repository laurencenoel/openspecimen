// function to set focus on first element
function setFocusOnFirstElement()
	{
		frm = document.forms[0];
		if (frm != null)
		{
			for (i = 0 ; i < frm.elements.length; i++)
				{
				if (frm.elements[i].type != null &&  frm.elements[i].type != "hidden") 	
				{
					frm.elements[i].focus();	
					break;
				} 
				else {
					continue;
				}
			}
		}
	}

// function for Logout tab
	
	function MM_swapImgRestore() { 
	  var i,x,a=document.MM_sr; for(i=0;a&&i<a.length&&(x=a[i])&&x.oSrc;i++) x.src=x.oSrc;
	}

	function MM_findObj(n, d) { 
	  var p,i,x;  if(!d) d=document; if((p=n.indexOf("?"))>0&&parent.frames.length) {
		d=parent.frames[n.substring(p+1)].document; n=n.substring(0,p);}
	  if(!(x=d[n])&&d.all) x=d.all[n]; for (i=0;!x&&i<d.forms.length;i++) x=d.forms[i][n];
	  for(i=0;!x&&d.layers&&i<d.layers.length;i++) x=MM_findObj(n,d.layers[i].document);
	  if(!x && d.getElementById) x=d.getElementById(n); return x;
	}

	function MM_swapImage() { 
	  var i,j=0,x,a=MM_swapImage.arguments; document.MM_sr=new Array; for(i=0;i<(a.length-2);i+=3)
	   if ((x=MM_findObj(a[i]))!=null){document.MM_sr[j++]=x; if(!x.oSrc) x.oSrc=x.src; x.src=a[i+2];}
	}


function openInstitutionWindow()
{
	institutionwindow=dhtmlmodal.open('Institution', 'iframe', 'Institution.do?operation=add&pageOf=pageOfPopUpInstitution','Administrative Data', 'width=530px,height=175px,center=1,resize=0,scrolling=1')

    institutionwindow.onclose=function()
	{ 
		return true;
	}
}

function openDepartmentWindow()
{
    departmentWindow=dhtmlmodal.open('Department', 'iframe', 'Department.do?operation=add&pageOf=pageOfPopUpDepartment&menuSelected=3&submittedFor=AddNew','Administrative Data', 'width=530px,height=175px,center=1,resize=0,scrolling=1')
    
    departmentWindow.onclose=function()
	{
       return true;
	}
}

function openCRGWindow()
{
    crgWindow=dhtmlmodal.open('Cancer Research Group', 'iframe', 'CancerResearchGroup.do?operation=add&pageOf=pageOfPopUpCancerResearchGroup&menuSelected=4&submittedFor=AddNew','Administrative Data', 'width=530px,height=175px,center=1,resize=0,scrolling=1')

    crgWindow.onclose=function()
	{
	   return true;
	}
}

// function for storage container to view map

	function showStorageContainerMap()
    {	
        var frameUrl='ShowFramedPage.do?pageOf=pageOfSpecimen&storageType=-1';	
		platform = navigator.platform.toLowerCase();
		if (platform.indexOf("mac") != -1)
		{
		    NewWindow(frameUrl,'name',screen.width,screen.height,'no');
		}
		else
		{
		    NewWindow(frameUrl,'name','800','600','no');
	     }     
     }
     //to close window
	function closeUserWindow()
	{     
		  if(window.opener)
			 window.parent.close();
			
	}


	// for assignPriviledge
		var request;
   function sendRequestsWithData(url,data,operation)
   {
     	request=newXMLHTTPReq();
		request.onreadystatechange = function requestHandler(){
        if(request.readyState == 4)
		{       //When response is loaded
            if(request.status == 200)
			{   
	            var response = request.responseText; 
				var jsonResponse = eval('('+ response+')');
	            var hasValue = false;
				
				if(jsonResponse.locations!=null)
				{
					var num = jsonResponse.locations.length; 
				
					if(operation == "getUsersForThisSites"){	
						var eleOfUserSelBox = document.getElementById('userIds');
						while(eleOfUserSelBox.length > 0){
							eleOfUserSelBox.remove(eleOfUserSelBox.length - 1);
						}
						var myNewOption = new Option("--All--","-1");	
						document.getElementById('userIds').options[0] = myNewOption;
					}	
					else if(operation == "getActionsForThisRole"){
						var eleOfActionSelBox = document.getElementById('actionIds');	
						for(var opt=0; opt<eleOfActionSelBox.length; opt++){
							eleOfActionSelBox.options[opt].selected=false;
						}
					}	
					for(i=0;i<num;i++)
					{						
						if(operation == "getUsersForThisSites"){	
							theValue  = jsonResponse.locations[i].locationId;
							theText = jsonResponse.locations[i].locationName;
						
							var myNewOption = new Option(theText,theValue);	
							
							document.getElementById('userIds').options[i+1] = myNewOption;
						}
						else if(operation == "getActionsForThisRole"){	
							theValue = jsonResponse.locations[i].locationId;
							theText  = jsonResponse.locations[i].locationName;
						
							var elSel = document.getElementById('actionIds');	
							for(var opt=0; opt<elSel.length; opt++){
							
									if(elSel.options[opt].value==theValue){
										elSel.options[opt].selected=true;
									}
							}
						}

						else if(operation == "addPriviledge"){
					
							var tableId="summaryTableId";
							var rowId=jsonResponse.locations[i].rowId; 
							var userName=jsonResponse.locations[i].userName;
					
							var userId=jsonResponse.locations[i].userId;
							var roleId=jsonResponse.locations[i].roleId; 
							var sites=jsonResponse.locations[i].sites;
							// for update table after response
				
							var selectedRowId=document.getElementById(rowId);
							
								if(selectedRowId!=null){
									// replace row to table
									var opt="updateRow";
								}
								else{
									// add row to table
									var opt="addRow";
								}
								addOrUpdateRowToTable(opt,tableId,rowId,userName,userId,sites);
						}
					}
				}
              }
          }
      };
		request.open("POST",url,true);
		request.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
		request.send(data);
   	}
   
   
	function getUsersForThisSites(siteObject)
	{	
			var operation="getUsersForThisSites";
		//	var operation= form.operation.value;
		//	alert(operation);
		    var selectedSiteIds = new Array();
			var count=0;
			for (var i = 0; i < siteObject.options.length; i++)
			{
				if (siteObject.options[ i ].selected){
				    selectedSiteIds[count]=(siteObject.options[i].value);
					count=count+1;
					//	var pcrType = ctrl.options[ctrl.selectedIndex].text;					
					//	var pcrTypeId = ctrl.options[ctrl.selectedIndex].value;	
					
				}
			}
			var url="ShowAssignPrivilegePage.do";
			var data="operation="+operation+"&selectedSiteIds="+selectedSiteIds;
		
			sendRequestsWithData(url,data,operation);
	}
	function getActionsForThisRole(roleObject)
	{		
			var form=roleObject.form;
			var operation="getActionsForThisRole";				 
			var selectedRoleType = roleObject.options[roleObject.selectedIndex].text;			 		
			var selectedRoleIds = roleObject.options[roleObject.selectedIndex].value;
		
			var url="ShowAssignPrivilegePage.do";
			var data="operation="+operation+"&selectedRoleIds="+selectedRoleIds;			
			sendRequestsWithData(url,data,operation);
				
	}
	function getUserPriviledgeSummary()
	{
		var operation="addPriviledge";	
		siteListCtrl=document.getElementById("siteIds");
		userListCtrl=document.getElementById("userIds");
		roleListCtrl=document.getElementById("roleIds");
		actionListCtrl=document.getElementById("actionIds");
		
		 var selectedSiteIds = new Array();
		 var selectedUserIds = new Array();
		 var selectedRoleIds = new Array();
		 var selectedActionIds = new Array();
			for (var i = 0; i < siteListCtrl.options.length; i++)
			{
				if (siteListCtrl.options[ i ].selected){
				    selectedSiteIds.push(siteListCtrl.options[ i ].value);
				}
			}
			if(((userListCtrl.options[ 0 ].value)=="-1")&&(userListCtrl.options[ 0 ].selected)){
				for (var i = 1; i < userListCtrl.options.length; i++)
				{
					 selectedUserIds.push(userListCtrl.options[ i ].value);
				}
			}
			else {
				for (var i = 0; i < userListCtrl.options.length; i++)
				{
					if (userListCtrl.options[ i ].selected){
					    selectedUserIds.push(userListCtrl.options[ i ].value);
					}
				}
			}	
			for (var i = 0; i < roleListCtrl.options.length; i++)
			{
				if (roleListCtrl.options[ i ].selected){
				    selectedRoleIds.push(roleListCtrl.options[ i ].value);
				}
			}
			if(((actionListCtrl.options[ 0 ].value)=="-1")&&(actionListCtrl.options[ 0 ].selected)){
				
					for (var i = 1; i < actionListCtrl.options.length; i++)
					{
						 selectedActionIds.push(actionListCtrl.options[ i ].value);
					}
				
			}
			else{	
				for (var i = 0; i < actionListCtrl.options.length; i++)
				{
					if (actionListCtrl.options[ i ].selected){
					    selectedActionIds.push(actionListCtrl.options[ i ].value);
					}
				}	
			}
			var url="ShowAssignPrivilegePage.do";
			var data="operation="+operation+"&selectedSiteIds="+selectedSiteIds+"&selectedUserIds="+selectedUserIds+"&selectedRoleIds="+selectedRoleIds+"&selectedActionIds="+selectedActionIds;			
			sendRequestsWithData(url,data,operation);	
	}

	function addOrUpdateRowToTable(opt,tableId,rowId,userName,userId,sites)
	{
		var tb = document.getElementById(tableId);
		var rows = new Array(); 
		rows = tb.rows;
		var noOfRows = rows.length;	
		if(opt=="addRow")
		{
			var row = document.getElementById(tableId).insertRow(noOfRows);
			row.setAttribute("id",rowId);
	//	    row.onclick=function(){editRow(rowId);} ;
			if(noOfRows%2!=0){
				row.setAttribute("class","tabletd1");
			}
			var chkName="chk_"+noOfRows;
		}
		else if(opt=="updateRow")
		{
			var row = document.getElementById(rowId);
			chkName=row.firstChild.firstChild.id;
			 while (row.childNodes.length > 0) {
				 row.removeChild(row.firstChild);
			 } 
			row.setAttribute("id",rowId);
			
		}
		  // first cell
			var aprCheckb=row.insertCell(0);
			aprCheckb.className="black_ar";			
			aprCheckb.width="13%";			
			sname="<input type='checkbox' name='" + chkName +"' id='" + chkName +"'/>";		
			aprCheckb.onclick=function(){enableDeleteButton(this.firstChild);} ;
			aprCheckb.innerHTML=""+sname;

				// Second Cell
			var aprSites=row.insertCell(1);
			aprSites.className="black_ar";
			aprSites.width="24%";
			aprSites.innerHTML="<label>"+sites+"</label>";
			

					//third Cell
			var aprUser=row.insertCell(2);
			aprUser.className="black_ar";
			aprUser.width="23%";
			aprUser.innerHTML="<label>"+userName+"</label>";

			//fourth Cell
			var aprRole=row.insertCell(3);
			aprRole.className="black_ar";
			aprRole.width="20%";
			ctrl=document.getElementById('roleIds');
			sname=ctrl.options[ctrl.selectedIndex].text;
			aprRole.innerHTML="<label>"+sname+"</label>";

			//Fifth Cell
			var aprActions=row.insertCell(4);
			aprActions.className="black_ar";
			aprActions.width="20%";
			aprActions.innerHTML="<label>action1,action2</label>";
	}

	
 function editRow(rowId){
	
	 var operation="editRow";
//	 var url="ShowAssignPrivilegePage.do";
//			var data="operation="+operation+"&selectedRow="+selectedRow;
		
//			sendRequestsWithData(url,data,operation);
 }

function enableDeleteButton(itemCheck){
	deleteButton=document.apForm.deleteButton;
	var chk = document.getElementById(itemCheck.id);
	        if (chk.checked == true)
	        {
	        	deleteButton.disabled = false;
	        }
}
function  deleteCheckedRows() {
	/** element of tbody    **/
	var tbodyElement = document.getElementById('summaryTableId');
	/** number of rows present    **/
	var counts = tbodyElement.rows.length;
	var deletedRowsArray=new  Array();
	var arrayCounter=0;
	var operation="deleteRow";
	
	for(var i=0;i < counts;i++)
	{
		itemCheck ="chk_"+i;
		var chk = document.getElementById(itemCheck);
	
		if(chk.checked==true){
			var pNode = null;
			var k = 0;

				/** getting table ref from tbody    **/
				pNode = tbodyElement.parentNode;
				
				/** curent row of table ref **/
				var currentRow = chk.parentNode.parentNode;
				k = currentRow.rowIndex;
				/** deleting row from table **/
				pNode.deleteRow(k);
			deletedRowsArray[arrayCounter]=currentRow.id;
			arrayCounter=arrayCounter+1;
		}
		if(tbodyElement.rows.length==0){
			document.apForm.deleteButton.disabled = true;
		}
	}

	var index=0;
	for(var i=0;i < counts;i++){
		var chk = document.getElementById("chk_"+i);
		if(chk!=null){
			chk.id="chk_"+(i-index);
	
			if((i-index)%2!=0){
				chk.parentNode.parentNode.setAttribute("class","tabletd1");
			}
			else{
				chk.parentNode.parentNode.setAttribute("class","black_ar");
			}
		}
		else{
			index=index+1;
		}
	}
	var url="ShowAssignPrivilegePage.do";
	var data="operation="+operation+"&deletedRowsArray="+deletedRowsArray;			
	sendRequestsWithData(url,data,operation);
}