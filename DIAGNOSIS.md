List of issues:
Fix group search and display
Problem: Unable to find groups.
Task: Restore group search/browser functionality.
Comment: Critical, as a group needs to be added for testing.
Fix "Add Contact" field validation
Problem: The field only accepts numbers, although it should support nicknames.
Task: Remove the restriction on entering numbers, allow alphabetic characters and special characters for nicknames.
Fix home screen search
Problem: Search is not working.
Task: Refine the search logic on the home page, ensure relevant results are returned.
Fix contact selection
Problem: Selecting a person to write a message to does not work.
Task: Fix the mechanism for selecting a contact from the list to start a conversation.
Fix sending messages from the contact list
Problem: Cannot message a person from the contact list.
Task: Restore the ability to open the chat and send messages from the contacts section. Fix sending messages from the home screen
Problem: Unable to message a person from the home screen.
Task: Check and fix quick access buttons/actions to chat from the home page.
Investigate the issue with incoming message delivery
Problem: Incoming message not received.
Task: Check the push notification logic, WebSocket connection, or polling mechanism for receiving messages.
Conduct regression testing
Task: After fixing the bugs, test the full workflow: adding a contact, searching for a group, sending and receiving messages.
Recommendation: Start with tasks 1 and 2, as they block the addition of test data.
