/**
 *    '$RCSfile: EcogridEndPointInterface.java,v $'
 *
 *     '$Author: tao $'
 *       '$Date: 2006-11-06 18:17:19 $'
 *   '$Revision: 1.1 $'
 *
 *  For Details: http://kepler.ecoinformatics.org
 *
 * Copyright (c) 2003 The Regents of the University of California.
 * All rights reserved.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY
 * OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package org.ecoinformatics.datamanager.download;

/**
 * This interface provides a API for Data Manager to get Ecogrid endpoint information.
 * Any application which will use Data Manager Library should implement this interface.
 * @author tao
 *
 */
public interface EcogridEndPointInterface 
{
   /**
    * Gets the end point which Metacat implements ecogrid interface.
    * This end point will be used to handle ecogrid protocal
    * @return end point url string
    */
   public String getMetacatEcogridEndPoint();
   
   /**
    * Gets the end point which SRB implements ecogrid interface
    * This end point will be used to handle srb protocal
    * @return end point url string
    */
   public String getSRBEcogridEndPoint();
   
   /**
    * Gets the machine name which srb protocal will be used.
    * The default value for this class is "srb-mcat.sdsc.edu"
    * @return the machine name of srb server
    */
   public String getSRBMachineName();
}