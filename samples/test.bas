0  j=-1: k=1
10 A=1:B=1:C=0
11 print "A=" A  "B=" B  "C=" C
14 PRINT "TEST nested bracket,a=b:"
15 if ((a=b)) goto 40
16 print "TEST log. ,IF AND A=B AND (C=A-1): "
20 IF A=B AND (C=A-1) THEN GOTO 40 
25 PRINT "was false"
30 END
40 PRINT "was true"
41c=1
print "TEST log. negation IF not a=c"
42IF not a=c then goto 44
43print "a gleich c"
goto 45
44 print "c ungleich a"
45 A=9:B=4
46 A$="TEXT"
a% = 5
b% = 2
REM BLA
50 Print "TEST function len and integer division" ; len(a$) / 3
60 Print "TEST integer division: "; a%/b
c$ = "3.4"
d$ = "3"
a = 5
b = 2
PRINT "TEST GOSUB"
gosub 666
print "TEST definition self defined function:"
DEF K(j,k) = j+2 + 2*k
Print "TEST internal functions 2xval,left, right, mid: "val(c$+"5");" ";val(d$); " "; LEFt$(c$, 7); Right$(c$, 7); mid$(c$, 2,7)
prin
print "TEST call self defined function K="; K(2, 32)
print "TEST a / b ;" ", a/b
print "TEST inverted sign -a and -a%: "; -a ; -a%
print "TEST on gosub[1..4]: "
input a
on a gosub 80,90,100, 999 : REM on a goto 80,90,100,999
PRINT "TEST invalid inputs skip statement and String concatition: " + "Yippie"
goto 1000
80 print "80"
goto 1000
90 print "90"
goto 1000
100 print "100"
goto 1000
999:
return
666:
print "TEST AND RETURN"
return 
1000:
print "TEST power  2^2: " 2^2
print "TEST 27 / 3 /3" 27 / (2+1) /  3
print "TEST Brackets and Sign: -(2*b)=" -(2*b)
print "TEST bin. AND and OR with 2 AND 4 & 4 OR 2:" 2 AND 4 4 OR 2
print "TEST comparison outside if: 2=3, 3>0" 2=3  3>0