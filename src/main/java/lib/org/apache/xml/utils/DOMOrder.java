/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: DOMOrder.java,v 1.3 2004/02/17 04:21:14 minchau Exp $
 */
package lib.org.apache.xml.utils;

/**
 * @deprecated Since the introduction of the DTM, this class will be removed.
 * Nodes that implement this index can return a document order index.
 * Eventually, this will be replaced by DOM 3 methods. 
 * (compareDocumentOrder and/or compareTreePosition.)
 */
public interface DOMOrder
{

  /**
   * Get the UID (document order index).
   *
   * @return integer whose relative value corresponds to document order
   * -- that is, if node1.getUid()<node2.getUid(), node1 comes before
   * node2, and if they're equal node1 and node2 are the same node. No
   * promises are made beyond that.
   */
  public int getUid();
}