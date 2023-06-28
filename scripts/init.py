import sys
import json
import psycopg2


def main(filepath):
    with open(filepath) as file:
        data = json.load(file)


    # Extract connection details
    connection_details = data['connection']

    host = connection_details['host']
    port = connection_details['port']
    database = connection_details['database']
    username = connection_details['username']
    password = connection_details['password']

    print("Connecting to database...")

    # Connect to the database
    connection = psycopg2.connect(
        host=host,
        port=port,
        database=database,
        user=username,
        password=password
    )
    cursor = connection.cursor()

    print("Connection established: host=" + host +
            ", port=" + port + ", database=" +
            database + ", username=" +
            username + ", password=" + password)

    # Iterate over towers
    towers = data['towers']

    for tower_data in towers:
        position = tower_data['position']
        x = position['x']
        y = position['y']

        # Insert position into database
        cursor.execute("INSERT INTO positions(x, y) VALUES (%s, %s) RETURNING id", (x, y))
        position_id = cursor.fetchone()[0]

        # Insert tower into the database
        cursor.execute("INSERT INTO towers(position_id) VALUES (%s) RETURNING id", (position_id,))
        tower_id = cursor.fetchone()[0]

        # Update tower id of position in database
        cursor.execute("UPDATE positions SET tower_id = %s WHERE id = %s", (tower_id, position_id))

        print("Tower with id " + str(tower_id) + " saved")

        # Store protection walls of tower
        protection_walls = tower_data['protectionWalls']
        for protection_wall_data in protection_walls:
            state = protection_wall_data['state']
            broken = state['broken']
            enchanted = state['enchanted']

            # Insert protection wall state into database
            cursor.execute("INSERT INTO protection_wall_states(broken, enchanted) VALUES (%s, %s) RETURNING id", (broken, enchanted))
            protection_wall_state_id = cursor.fetchone()[0]

            # Insert protection wall into the database
            cursor.execute("INSERT INTO protection_walls(tower_id, state_id) VALUES (%s, %s) RETURNING id", (tower_id, protection_wall_state_id))
            protection_wall_id = cursor.fetchone()[0]

            # Update protection wall id of protection wall state in database
            cursor.execute("UPDATE protection_wall_states SET protection_wall_id = %s WHERE id = %s", (protection_wall_id, protection_wall_state_id))
        print(str(len(protection_walls)) + " protection walls of tower with id " + str(tower_id) + " saved")


    print("Committing...")
    # Commit the changes and close the connection
    connection.commit()
    cursor.close()
    connection.close()
    print("====== Success! ======")



arguments = sys.argv[1:]

if (len(arguments) == 1):
    filepath = arguments[0]
    main(filepath)
else:
    raise RuntimeError("Expected json filepath. Got: " + arguments)