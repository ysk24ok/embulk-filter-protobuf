import com.example.tutorial.AddressBookProtos.Person;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import java.util.Base64;

// https://developers.google.com/protocol-buffers/docs/proto3
// https://developers.google.com/protocol-buffers/docs/javatutorial
public class GenerateTestData
{
    public static void main(String args[])
    {
        Person john = Person.newBuilder()
            .setId(1234)
            .setName("John Doe")
            .setEmail("jdoe@example.com")
            .addPhone(
                Person.PhoneNumber.newBuilder()
                    .setNumber("111-0000")
                    .setType(Person.PhoneType.MOBILE))
            .addPhone(
                Person.PhoneNumber.newBuilder()
                    .setNumber("555-4321")
                    .setType(Person.PhoneType.HOME))
            .build();
        Person jane = Person.newBuilder()
            .setId(1235)
            .setName("Jane Doe")
            .addPhone(
                Person.PhoneNumber.newBuilder()
                    .setNumber("999-8888")
                    .setType(Person.PhoneType.MOBILE))
            .build();
        // output json
        String johnAsJson = null;
        String janeAsJson = null;
        try {
            johnAsJson= JsonFormat.printer()
                .omittingInsignificantWhitespace()
                .print(john);
            janeAsJson= JsonFormat.printer()
                .omittingInsignificantWhitespace()
                .print(jane);
        }
        catch (InvalidProtocolBufferException e) {
            System.out.println(e);
        }
        System.out.println(String.format("John as JSON: %s", johnAsJson));
        System.out.println(String.format("Jane as JSON: %s", janeAsJson));
        // output encoded bytes
        System.out.println(String.format(
            "John as encoded bytes: %s",
            Base64.getEncoder().encodeToString(john.toByteArray())
        ));
        System.out.println(String.format(
            "Jane as encoded bytes: %s",
            Base64.getEncoder().encodeToString(jane.toByteArray())
        ));
    }
}
