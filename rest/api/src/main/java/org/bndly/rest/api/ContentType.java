package org.bndly.rest.api;

/*-
 * #%L
 * REST API
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface ContentType {

    String getName();
    String getExtension();
	
    public static final ContentType TEXT = new ContentType() {

        @Override
        public String getName() {
            return "text/plain";
        }

		@Override
		public String getExtension() {
			return "txt";
		}

    };
    public static final ContentType PDF = new ContentType() {

        @Override
        public String getName() {
            return "application/pdf";
        }

		@Override
		public String getExtension() {
			return "pdf";
		}

    };
    public static final ContentType JSON = new ContentType() {

        @Override
        public String getName() {
            return "application/json";
        }

		@Override
		public String getExtension() {
			return "json";
		}

    };
    public static final ContentType XML = new ContentType() {

        @Override
        public String getName() {
            return "application/xml";
        }

		@Override
		public String getExtension() {
			return "xml";
		}

    };
    public static final ContentType MULTIPART_FORM_DATA = new ContentType() {

        @Override
        public String getName() {
            return "multipart/form-data";
        }

		@Override
		public String getExtension() {
			return null;
		}

    };
    public static final ContentType WILD_CARD = new ContentType() {

        @Override
        public String getName() {
            return "*/*";
        }
		
		@Override
		public String getExtension() {
			return null;
		}

    };
    public static final ContentType HTML = new ContentType() {

        @Override
        public String getName() {
            return "text/html";
        }
		
		@Override
		public String getExtension() {
			return "html";
		}

    };
}
