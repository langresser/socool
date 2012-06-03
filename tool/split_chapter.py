import re,os;

def split_file(file):
    fp = open(file, 'r');
    dir_name = file[:file.rfind('.')];

    if not os.path.exists(dir_name):
        os.makedirs(dir_name);

    all_text = [];
    data_text = [];

    for line in fp:
        if re.search('.*半部.*第.*集.*第.*章', line):
            if len(data_text) > 0:
                all_text.append(data_text);

            data_text = [];
            data_text.append(line);
            continue;

        data_text.append(line);
    fp.close();

    fpwd = open('{0}/data.txt'.format(dir_name), 'w');
    

    i = -1;
    for each in all_text:
        i += 1;
        title_line = each[0][each[0].find('第') :].strip();
        first_di = title_line.find('第');
        second_di = title_line.find('第', first_di + 1);
        juan = title_line[:second_di];
        zhang = title_line[title_line.rfind('第'):];
        
        each[0] = zhang;
        
        size = 0;
        for eee in each:
            size += len(eee);

        if len(each) < 5:
            print(each[0]);

        fpwd.write('{0}.txt@@{1}@@{2}@@{3}@@{4}\n'.format(i, len(each), size, zhang, juan));


        fpw = open('{0}/{1}.txt'.format(dir_name, i), 'w');
        fpw.writelines(each);
        fpw.close();

    fpwd.close();
split_file('无限恐怖.txt');
