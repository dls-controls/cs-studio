package org.csstudio.nams.service.messaging.declaration;

public interface ProducerFactoryService {

	
	public Producer createProducer(String clientId, String messageDestinationName, PostfachArt artDesPostfaches, String[] messageServerURLs);
}
