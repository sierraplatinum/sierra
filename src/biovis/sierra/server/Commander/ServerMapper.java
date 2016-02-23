/**
 *****************************************************************************
 * Copyright (c) 2015 Daniel Gerighausen, Lydia Mueller, and Dirk Zeckzer
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
 ******************************************************************************
 */
package biovis.sierra.server.Commander;

import biovis.sierra.data.DataMapper;
import biovis.sierra.server.SuperDuperPeakCaller;
/**
 *
 * @author Daniel Gerighausen
 */
public class ServerMapper {
	private transient SuperDuperPeakCaller spc;
	private DataMapper mapper = new DataMapper();
	private String passwd = "";
	private boolean locked = false;
	private int chunksize = 10000;
	private int IOThreads = 6;

	public SuperDuperPeakCaller getSpc() {
		return spc;
	}

	public void setSpc(SuperDuperPeakCaller spc) {
		this.spc = spc;
	}

	public DataMapper getMapper() {
		//		System.err.println(mapper.hashCode());
		return mapper;
	}

	public void setMapper(DataMapper mapper) {
		this.mapper = mapper;
		//		System.err.println(mapper.hashCode());
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public int getChunksize() {
		return chunksize;
	}

	public void setChunksize(int chunksize) {
		this.chunksize = chunksize;
	}

	public int getIOThreads() {
		return IOThreads;
	}

	public void setIOThreads(int iOThreads) {
		IOThreads = iOThreads;
	}
}
