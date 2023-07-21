#! /bin/bash

echo -n "Are you sure you want to recreate all global LAD databases? (y/n)"
read CONFIRM

if test "$CONFIRM" = "y" -o "$CONFIRM" = "Y"
then
    CONFIRM=y
    echo 'Destroying and recreating all global LAD databases...'
    
    echo -e "y\n" | ${CHILL_GDS}/bin/admin/chill_destroy_lad_unit_test_database -u root 
    echo -e "y\n" | ${CHILL_GDS}/bin/admin/chill_grant_lad_unit_test_permissions -u root
    echo -e "y\n" | ${CHILL_GDS}/bin/admin/chill_create_lad_unit_test_database -u root

    echo -e "y\n" | ${CHILL_GDS}/bin/admin/chill_destroy_lad_database -u root
    echo -e "y\n" | ${CHILL_GDS}/bin/admin/chill_grant_lad_permissions -u root
    echo -e "y\n" | ${CHILL_GDS}/bin/admin/chill_create_lad_database -u root
    
    echo 'Finished destroying and recreating all global LAD databases.'
else
    echo "Aborting without any changes."
fi
