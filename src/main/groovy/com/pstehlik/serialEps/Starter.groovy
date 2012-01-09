package com.pstehlik.serialEps

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;


import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;


import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;


/**
 * Description missing
 *
 * @author Philip Stehlik
 * @since
 */
class Starter {
  private static final String CONFIG_FILE='serialeps.properties'

  private def arduinoPort
  private Session session
  private Store store
  private Folder inbox

  private Properties config

  public static void main(String[] args) {
    def starter = new Starter()
    starter.run()
  }

  private initConfig(){
    config = new Properties()
    config.load(this.class.classLoader.getResourceAsStream("serialeps.properties"))
  }

  private void run() {
    initConfig()
    Enumeration pList = CommPortIdentifier.getPortIdentifiers();
    def arduinoPortId = pList.find {it.portType == CommPortIdentifier.PORT_SERIAL && it.name == ardunioPortname}

    if (!arduinoPortId) {
      println "Couldn't find ardunio on port [${ardunioPortname}]."
      System.exit(-1)
    }
    try {
      arduinoPort = arduinoPortId.open("SimpleOnOffTest", 2000)
      arduinoPort.setSerialPortParams(9600,
                                      SerialPort.DATABITS_8,
                                      SerialPort.STOPBITS_1,
                                      SerialPort.PARITY_NONE)
      initEmailConnection()
      while (true) {
        openEmailAccount()
        println "Looking for new email at [${new Date()}]."
        lookAtNewMessages()
        if(isThereANewEPMail()){
          println "Wohoooo! New email matching [${magicSubject}]! Play some music and dance!!!"
          sendSignalToArduino('1')
        }
        markAllEmailsRead()
        closeEmailConnection()
        sleep((45 * 1000) as long )
      }
    } finally {
      if (null != arduinoPort) {
        arduinoPort.close()
      }
      closeEmailConnection()
    }
  }

  void sendSignalToArduino(String signal) {
    arduinoPort.outputStream << signal.bytes
  }


  void initEmailConnection(){
    Properties props = System.getProperties();
    props.setProperty("mail.store.protocol", "imaps");
    session = Session.getDefaultInstance(props, null);
  }

  void openEmailAccount(){
    store = session.getStore("imaps");
    store.connect("imap.gmail.com", emailUser, emailPassword);
    inbox = store.getFolder("Inbox");
    inbox.open(Folder.READ_WRITE);
  }

  void closeEmailConnection(){
    if(null != store){
      store.close()
    }
  }

  def getUnreadMessages(){
    FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
    inbox.search(ft);
  }

  void lookAtNewMessages(){
    getUnreadMessages().eachWithIndex{Message it, ix ->
      println "Found unread e-mail [${it.getSubject()}]."
    }
  }

  def markAllEmailsRead(){
    getUnreadMessages().eachWithIndex{Message it, ix ->
      println "Marking e-mail [${it.getSubject()}] as read."
      it.setFlag(Flags.Flag.SEEN, true)
    }
  }

  boolean isThereANewEPMail(){
    getUnreadMessages().find{it.subject.toUpperCase().contains(magicSubject.toUpperCase())} as boolean
  }


  private String getArdunioPortname(){
    config.getProperty('arduino.portname')
  }

  private String getEmailUser(){
    config.getProperty('email.user')
  }
  private String getEmailPassword(){
    config.getProperty('email.password')
  }
  private String getMagicSubject(){
    config.getProperty('email.magicSubject')
  }
}
